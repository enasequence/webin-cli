package uk.ac.ebi.ena.rawreads;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.ena.rawreads.RawReadsFile.AsciiOffset;
import uk.ac.ebi.ena.rawreads.RawReadsFile.ChecksumMethod;
import uk.ac.ebi.ena.rawreads.RawReadsFile.Filetype;
import uk.ac.ebi.ena.rawreads.RawReadsFile.QualityScoringSystem;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.AbstractWebinCli;
import uk.ac.ebi.ena.webin.cli.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.SubmissionBundle.PAYLOAD_TYPE;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

public class 
RawReadsWebinCli extends AbstractWebinCli
{   
    private static final String RUN_XML = "run.xml";
    List<RawReadsFile> files;
    private String  experiment_id;
    private boolean valid;
    private boolean test_mode;
    private File    submit_dir;
    private File    validate_dir;
    

    @Override public void 
    init( WebinCliParameters parameters ) throws ValidationEngineException
    {
        try
        {
            super.init( parameters );

            defineFileTypes( getParameters().getManifestFile() );
//            setAssemblyInfo( defineInfo( infoFile ) );
//            setName( getAssemblyInfo().getName().trim().replaceAll( "\\s+", "_" ) );
            
            setValidationDir( createOutputSubdir( String.valueOf( ContextE.reads ), getName(), VALIDATE_DIR ) );
            setSubmitDir( createOutputSubdir( String.valueOf( ContextE.reads ), getName(), SUBMIT_DIR ) );
            
        } catch( ValidationEngineException | WebinCliException e )
        {
            throw e;
        } catch( Throwable t )
        {
            throw new ValidationEngineException( "Unable to init validator", t );
        }
    }
    
    
    RawReadsFile
    parseFileLine( String[] tokens )
    {
        RawReadsFile result = new RawReadsFile();
        result.setInputDir( getParameters().getInputDir().toPath() );
        
        for( String token : tokens )
        {
            token = token.trim();
            switch( token )
            {
            case "FASTQ":
            case "BAM":
            case "CRAM":
                result.setFiletype( Filetype.valueOf( token.toLowerCase() ) );
                break;
                
            case "PHRED_33":
                result.setAsciiOffset( AsciiOffset.FROM33 );
                result.setQualityScoringSystem( QualityScoringSystem.phred );
                break;
                
            case "PHRED_64":
                result.setAsciiOffset( AsciiOffset.FROM64 );
                result.setQualityScoringSystem( QualityScoringSystem.phred );
                break;
                
            case "LOGODDS":
                result.setAsciiOffset( null );
                result.setQualityScoringSystem( QualityScoringSystem.log_odds );
                break;
            
            default:
                if( null != token && !token.isEmpty() )
                    result.setFilename( token );
            }
        }
        
        if( !Paths.get( result.getFilename() ).isAbsolute() )
            result.setFilename( result.getInputDir().resolve( Paths.get( result.getFilename() ) ).toString() );
        
        return result;
    }
    
    
    void
    defineFileTypes( File manifest_file ) throws IOException
    {
        List<String> lines = Files.readAllLines( manifest_file.toPath() );
        List<RawReadsFile> files = new ArrayList<>();
        for( String line : lines )
        {
            String tokens[] = line.split( "\\s+" );
            String token0 = tokens[ 0 ];
            switch( token0 )
            {
            case "EXPERIMENT":
            case "EXPERIMENT-ID":
                this.experiment_id = tokens[ 1 ]; 
                
                break;
                
            case "NAME":
                setName( tokens[ 1 ] );
                break;
                
            default:
                files.add( parseFileLine( tokens ) );
            }
        }
        
        if( null == getName() || getName().isEmpty() )
            setName( String.format( "%s-%s", this.experiment_id, System.currentTimeMillis() ) );
        this.files = files;
    }
    
    
	private void
    setSubmitDir( File submit_dir )
    {
	    this.submit_dir = submit_dir;
    }


    private void
    setValidationDir( File validate_dir )
    {
        this.validate_dir = validate_dir; 
    }


    public void 
	setTestMode( boolean test_mode ) 
	{
	    this.test_mode = test_mode;
	}
	

	public boolean 
	getTestMode() 
	{
		return test_mode;
	}


    @Override public boolean
    validate() throws ValidationEngineException
    {
        this.valid = true;
        return valid;
    }


    @Override public SubmissionBundle
    getSubmissionBundle()
    {
        if( valid )
        {
            try
            {
                prepareSubmissionBundle();
            } catch( IOException ioe )
            {
                throw new RuntimeException( ioe );
            }
        }
        return super.getSubmissionBundle();
    }

    
    void
    prepareSubmissionBundle() throws IOException
    {
        List<File> uploadFileList = files.stream().map( e -> new File( e.getFilename() ) ).collect( Collectors.toList() );
        Path uploadDir = Paths.get( String.valueOf( ContextE.reads ), getName() );
        files.forEach( e -> e.setChecksumMethod( ChecksumMethod.MD5 ) );
        files.forEach( e -> {
            try
            {
                e.setChecksum( FileUtils.calculateDigest( String.valueOf( e.getChecksumMethod() ), new File( e.getFilename() ) ) );
            } catch( NoSuchAlgorithmException | IOException e1 )
            {
                throw new RuntimeException( e1 );
            }
        } );
        List<Element> eList = files.stream()
                                   .sequential()
                                   .map( e -> e.toElement( "FILE", uploadDir ) )
                                   .collect( Collectors.toList() );

        //do something
        String xml = createRunXml( eList, experiment_id, getParameters().getCenterName() );
        
        Path runXmlFile = getSubmitDir().toPath().resolve( RUN_XML );
        Files.write( runXmlFile, xml.getBytes( StandardCharsets.UTF_8 ), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC );
        setSubmissionBundle( new SubmissionBundle( getSubmitDir(), 
                                                   uploadDir.toString(), 
                                                   uploadFileList, 
                                                   runXmlFile.toFile(), 
                                                   PAYLOAD_TYPE.RUN,
                                                   getParameters().getCenterName() ) );   
    }


    private File
    getSubmitDir()
    {
        return submit_dir;
    }


/*
    <RUN_SET>
    <RUN alias="" center_name="" run_center="blah">
        <EXPERIMENT_REF refname="" />
        <DATA_BLOCK>
            <FILES>
                <FILE filename="test_forward.fastq.gz" filetype="fastq" checksum="5aeca824118be49da0982bef9b57e689" checksum_method="MD5" quality_scoring_system="phred" ascii_offset="!" quality_encoding="ascii">
                    <READ_LABEL>F</READ_LABEL>
                </FILE>
                <FILE filename="test_reverse.fastq.gz" filetype="fastq" checksum="216e1803c0f22825caa58aa3622a0be5" checksum_method="MD5" quality_scoring_system="phred" ascii_offset="!" quality_encoding="ascii">
                    <READ_LABEL>R</READ_LABEL>
                </FILE>
            </FILES>
        </DATA_BLOCK>
    </RUN>
    </RUN_SET>
*/

    String
    createRunXml( List<Element> fileList, String experiment_id, String centerName  ) 
    {
        try 
        {
            String full_name = ContextE.reads.getTitle( getName() );
            Element runSetE = new Element( "RUN_SET" );
            Element runE = new Element( "RUN" );
            runSetE.addContent( runE );
            
            Document doc = new Document( runSetE );
            runE.setAttribute( "alias", "ena-RUN-" + System.currentTimeMillis() );
            
            if( null != centerName && !centerName.isEmpty() )
                runE.setAttribute( "center_name", centerName );
            
            runE.addContent( new Element( "TITLE" ).setText( full_name ) );
            Element experimentRefE = new Element( "EXPERIMENT_REF" );
            runE.addContent( experimentRefE );
            experimentRefE.setAttribute( "accession", experiment_id );
            
            Element dataBlockE = new Element( "DATA_BLOCK" );
            runE.addContent( dataBlockE );
            Element filesE = new Element( "FILES" );
            dataBlockE.addContent( filesE );
            
            for( Element e: fileList )
                filesE.addContent( e );
            
            XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat( Format.getPrettyFormat() );
            StringWriter stringWriter = new StringWriter();
            xmlOutput.output( doc, stringWriter );
            return stringWriter.toString();
            
        } catch( IOException e ) 
        {
            throw WebinCliException.createSystemError( e.getMessage() );
        }
    }
}
