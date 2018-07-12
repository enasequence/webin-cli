package uk.ac.ebi.ena.assembly;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import uk.ac.ebi.embl.agp.reader.AGPFileReader;
import uk.ac.ebi.embl.agp.reader.AGPLineReader;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.fasta.reader.FastaFileReader;
import uk.ac.ebi.embl.fasta.reader.FastaLineReader;
import uk.ac.ebi.embl.flatfile.reader.EntryReader;
import uk.ac.ebi.embl.flatfile.reader.embl.EmblEntryReader;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.AssemblyInfoReader;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.ChromosomeListFileReader;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.UnlocalisedListFileReader;
import uk.ac.ebi.ena.manifest.FileFormat;
import uk.ac.ebi.ena.manifest.ManifestFileReader;
import uk.ac.ebi.ena.manifest.ManifestObj;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.submit.SubmissionBundle;
import uk.ac.ebi.ena.submit.SubmissionBundle.SubmissionXMLFile;
import uk.ac.ebi.ena.submit.SubmissionBundle.SubmissionXMLFileType;
import uk.ac.ebi.ena.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.AbstractWebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

public abstract class 
SequenceWebinCli extends AbstractWebinCli
{
    private static final String DIGEST_NAME = "MD5";
    protected final static String ANALYSIS_XML = "analysis.xml";
    private final static String INVALID_INFO = "Info file validation failed. Please check the report file for errors: ";
    
    protected File   validationDir;
    protected File   submitDir;
    
    protected AssemblyInfoEntry assembly_info;
    protected File       infoFile;
    protected File       chromosomeListFile;
    protected File       unlocalisedListFile;
    protected List<File> fastaFiles;
    protected List<File> flatFiles;
    protected List<File> agpFiles;
    protected List<File> tsvFiles;
    private Study  study;
    private Sample sample;
    private Map<Integer, Integer> line_number_map;
    
    
    protected abstract boolean validateInternal() throws ValidationEngineException;
    
    
    protected File
    getReportFile( FileFormat filetype, String filename )
    {
        return super.getReportFile( String.valueOf( filetype ), filename );
    }
    
    
    protected List<File> 
    checkFiles( List<File> files, boolean compressed, String...suffixes ) throws ValidationEngineException
    {
        for( File f : files )
        {
            if( !f.exists() )
                throw new ValidationEngineException( String.format( "File %s does not exist", f.getPath() ) );
        
            if( f.isDirectory() )
                throw new ValidationEngineException( String.format( "File %s is a directory", f.getPath() ) );
            
            if( !f.canRead() )
                throw new ValidationEngineException( String.format( "Cannot read file %s", f.getPath() ) );

            if( !getTestMode() && compressed )
            {
compression:    do
                {
                    try( InputStream is = new FileInputStream( f ) )
                    {
                        try( GZIPInputStream gz = new GZIPInputStream( is ) )
                        {
                            break compression;
                        } catch( IOException ioe )
                        {
                            try( BZip2CompressorInputStream bz2 = new BZip2CompressorInputStream( is ) )
                            {
                                break compression;
                            }
                        }
                    } catch( IOException ioe )
                    {
                        throw new ValidationEngineException( String.format( "File %s should be compressed with GZip or with BZip2", f.getPath() ) );
                    }
                } while( false );
            }

if( !getTestMode() )
{            
suffix:     while( suffixes.length > 0 )
            {
                for( String suffix : suffixes )
                {
                    if( compressed )
                    {
                        if( f.getName().matches( "^.*\\" + suffix + ".*" ) ) 
                            break suffix;
                    } else
                    {
                        if( f.getName().endsWith( suffix ) ) 
                            break suffix;
                    }    
                }       
                throw new ValidationEngineException( String.format( "File %s should have an extention one of %s", f.getPath(), Arrays.asList( suffixes ) ) );
            }
        }
 }       
        return files;
    }
    
    
    protected void 
    defineFileTypes( File manifest_file ) throws ValidationEngineException, IOException 
    {
        ManifestFileReader reader = new ManifestFileReader();
        reader.read( manifest_file.getPath() );
        Map<Integer, Integer> line_number_map = new HashMap<>();
        List<File> fastaFiles = new ArrayList<>();
        List<File> flatFiles  = new ArrayList<>();
        List<File> agpFiles   = new ArrayList<>();
        List<File> tsvFiles   = new ArrayList<>();
        File infoFile = null; 
        File __infoFile = File.createTempFile( manifest_file.getName() + ".", ".info" );
        int __info_lineno = 1;
        
        for( ManifestObj obj : reader.getManifestFileObjects() )
        {
            if( null == obj.getFileFormat() )
            {
            	Files.write( __infoFile.toPath(), 
				     	     String.format( "%s\n", String.valueOf( obj ) ).getBytes( StandardCharsets.UTF_8 ),
				             StandardOpenOption.APPEND, StandardOpenOption.CREATE, StandardOpenOption.SYNC );
            	line_number_map.put( __info_lineno ++, obj.getLineNo() );
            	continue;
            }

            String fileName = obj.getFileName();
            File file = Paths.get( fileName ).isAbsolute() ? new File( fileName ) : new File( getParameters().getInputDir(), fileName );
            
            switch( obj.getFileFormat() ) 
            {
            case CHROMOSOME_LIST:
                chromosomeListFile = file;
                break;
            case UNLOCALISED_LIST:
                unlocalisedListFile = file;
                break;
            case FASTA:
                fastaFiles.add(file);
                break;
            case FLATFILE:
                flatFiles.add(file);
                break;
            case AGP:
                agpFiles.add(file);
                break;
            case TAB:
                tsvFiles.add( file );
                break;
            case INFO:
                infoFile = file;
                break;                
            default:
            	break;
            }
        }
        
        this.fastaFiles = checkFiles( fastaFiles, true, ".fasta", ".fas", ".fsa", ".fna", ".fa" );
        this.flatFiles  = checkFiles( flatFiles, true );
        this.agpFiles   = checkFiles( agpFiles, true, ".agp" );
        this.tsvFiles   = checkFiles( tsvFiles, true, ".tab", ".tsv" );
        this.infoFile   = null == infoFile ? __infoFile : infoFile; 
        this.line_number_map = line_number_map;
    }

    
    protected AssemblyInfoEntry
    defineInfo( File info_file ) throws IOException, ValidationEngineException
    {
        InfoFileValidator infoValidator = new InfoFileValidator();
        
        File reportFile = getReportFile( "", line_number_map.isEmpty() ? infoFile.getName() : getParameters().getManifestFile().getName() );
        
        if( infoValidator.read( infoFile, line_number_map ) )
        {
            AssemblyInfoEntry assembly_info = infoValidator.getAssemblyEntry();
            reportFile = getReportFile( FileFormat.INFO, assembly_info.getName() );
            
            if( !infoValidator.validate( getContext() ) )
            {
                FileUtils.writeReport( reportFile, infoValidator.getValidationResult() );
                throw WebinCliException.createUserError( INVALID_INFO, reportFile.getPath() );
            }
            
            return assembly_info;
            
        } else
        {
            FileUtils.writeReport( reportFile, infoValidator.getValidationResult() );
            throw WebinCliException.createUserError( INVALID_INFO, reportFile.getPath() );
        }
    }
    
    
    abstract ContextE getContext();
    
    
    @Override public void 
    init( WebinCliParameters parameters ) throws ValidationEngineException
    {
        try
        {
            
            super.init( parameters );

            setValidationDir( createOutputSubdir( "." ) );
            defineFileTypes( getParameters().getManifestFile() );
            if( null == infoFile )
                throw WebinCliException.createUserError( "Info file not defined" );
            
            if( !infoFile.exists() )
                throw WebinCliException.createUserError( String.format( "Info %s file does not exist", infoFile.getPath() ) );
            
            if( infoFile.isDirectory() )
                throw WebinCliException.createUserError( String.format( "Info %s file is directory", infoFile.getPath() ) );

            setAssemblyInfo( defineInfo( infoFile ) );
            setName( getAssemblyInfo().getName().trim().replaceAll( "\\s+", "_" ) );
            
            setValidationDir( createOutputSubdir( String.valueOf( getContext() ), getName(), VALIDATE_DIR ) );
            setSubmitDir( createOutputSubdir( String.valueOf( getContext() ), getName(), SUBMIT_DIR ) );

            if (getAssemblyInfo().getSampleId() != null)
                setSample( fetchSample( getAssemblyInfo().getSampleId(), getTestMode() ) );
            setStudy( fetchStudy( getAssemblyInfo().getStudyId(), getTestMode() ) );
        } catch( ValidationEngineException | WebinCliException e )
        {
            throw e;
        } catch( Throwable t )
        {
            throw new ValidationEngineException( "Unable to init validator", t );
        }
    }
    
    
    
    protected void
    setStudy( Study study )
    { 
        this.study = study;
    }




    protected void
    setSample( Sample sample )
    {
        this.sample = sample;
    }


    protected Study
    getStudy()
    { 
        return this.study;
    }


    protected Sample
    getSample()
    {
        return this.sample;
    }
    

    public void 
    setReportsDir( String reportDir )
    {
        this.validationDir = new File( reportDir );
    }
       
    
    @SuppressWarnings( "unchecked" ) protected <T> T
    getFileReader( FileFormat format, File file ) throws IOException
    {
         if( !EntryReader.getBlockCounter().isEmpty() )
             EntryReader.getBlockCounter().clear();
         
          if( !EntryReader.getSkipTagCounter().isEmpty() )
              EntryReader.getSkipTagCounter().clear();
          
        switch( format )
        {
        default:
            return null;
        
        case FASTA:
            return (T) new FastaFileReader( new FastaLineReader( FileUtils.getBufferedReader( file ) ) );

        case AGP:
            return (T) new AGPFileReader( new AGPLineReader( FileUtils.getBufferedReader( file ) ) );
        
        case FLATFILE:
            EmblEntryReader emblReader = new EmblEntryReader( FileUtils.getBufferedReader( file ), EmblEntryReader.Format.EMBL_FORMAT, file.getName() );
            emblReader.setCheckBlockCounts( true );
            return (T) emblReader;

        case INFO:
            return (T) new AssemblyInfoReader( file );

        case CHROMOSOME_LIST:
            return (T) new ChromosomeListFileReader( file );
            
        case UNLOCALISED_LIST:
            return (T) new UnlocalisedListFileReader( file );
        }
    }


    @Override public File
    getValidationDir()
    {
        return validationDir;
    }


    public File
    getSubmitDir()
    {
        return submitDir;
    }


    public void
    setValidationDir( File validationDir )
    {
        this.validationDir = validationDir;
    }


    public void
    setSubmitDir( File submitDir )
    {
        this.submitDir = submitDir;
    }


    public void 
    setInputDir( File inputDir )
    {
        getParameters().setInputDir( inputDir );
    }


    public File
    getInputDir()
    {
        return getParameters().getInputDir();
    }
    
    
    public AssemblyInfoEntry
    getAssemblyInfo()
    {
        return assembly_info;
    }


    public void
    setAssemblyInfo( AssemblyInfoEntry assembly_info )
    {
        this.assembly_info = assembly_info;
    }

    
    protected Element
    createTextElement( String name, String text )
    {
        Element e = new Element( name );
        e.setText( text );
        return e;
    }
   
    
   abstract Element makeAnalysisType( AssemblyInfoEntry entry );

   
    String
    createAnalysisXml( List<Element> fileList, AssemblyInfoEntry entry, String centerName  ) 
    {
        try 
        {
            String full_name = getContext().getTitle( getName() );
            //String type      = getContext().getType(); 
            
            Element analysisSetE = new Element( "ANALYSIS_SET" );
            Element analysisE = new Element( "ANALYSIS" );
            analysisSetE.addContent( analysisE );
            
            Document doc = new Document( analysisSetE );
            analysisE.setAttribute( "alias", "ena-ANALYSIS-" + System.currentTimeMillis() );
            
            if( null != centerName && !centerName.isEmpty() )
                analysisE.setAttribute( "center_name", centerName );
            
            analysisE.addContent( new Element( "TITLE" ).setText( full_name ) );
            Element studyRefE = new Element( "STUDY_REF" );
            analysisE.addContent( studyRefE );
            studyRefE.setAttribute( "accession", entry.getStudyId() );
            if( entry.getSampleId() != null && !entry.getSampleId().isEmpty() )
            {
                Element sampleRefE = new Element( "SAMPLE_REF" );
                analysisE.addContent( sampleRefE );
                sampleRefE.setAttribute( "accession", entry.getSampleId() );
            }
            Element analysisTypeE = new Element( "ANALYSIS_TYPE" );
            analysisE.addContent(analysisTypeE);
            Element typeE = makeAnalysisType( entry );
            analysisTypeE.addContent( typeE );

            Element filesE = new Element( "FILES" );
            analysisE.addContent( filesE );
            
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


    protected Element
    createfileElement( Path uploadDir, File file, String file_type )
    {
        try
        {
            return createfileElement( String.valueOf( uploadDir.resolve( extractSubpath( getParameters().getInputDir(), file ) ) ).replaceAll( "\\\\+", "/" ), 
                                      String.valueOf( file_type ), 
                                      DIGEST_NAME, 
                                      FileUtils.calculateDigest( DIGEST_NAME, file ) );
        }catch( IOException | NoSuchAlgorithmException e )
        {
            throw new RuntimeException( e );
        }
    }

    
    private Element
    createfileElement( String file_path, String file_type, String digest, String checksum )
    {
        Element fileE = new Element( "FILE" );
        fileE.setAttribute( "filename", file_path );
        fileE.setAttribute( "filetype", String.valueOf( file_type ) );
        fileE.setAttribute( "checksum_method", digest );
        fileE.setAttribute( "checksum", checksum );
        return fileE;
    }
    

    protected String
    extractSubpath( File inputDir, File file ) throws IOException
    {
        return file.toPath().startsWith( inputDir.toPath() ) ? inputDir.toPath().relativize( file.toPath() ).toString() : file.getName();
    }
    
    
    @Override public File
    getSubmissionBundleFileName()
    {
        return new File( getSubmitDir(), "validate.reciept" );
    }


    @Override public boolean
    validate() throws ValidationEngineException
    {
        if( !FileUtils.emptyDirectory( getValidationDir() ) )
            throw WebinCliException.createSystemError( "Unable to empty directory " + getValidationDir() );
        
        if( !FileUtils.emptyDirectory( getSubmitDir() ) )
            throw WebinCliException.createSystemError( "Unable to empty directory " + getSubmitDir() );
        
        return validateInternal();
    }
    
    
    protected List<File>
    getUploadFiles() throws IOException
    {
        List<File> uploadFileList = new ArrayList<>();
        if( null != chromosomeListFile )
            uploadFileList.add( chromosomeListFile );           
        
        if( null != unlocalisedListFile )
            uploadFileList.add( unlocalisedListFile );
        
        uploadFileList.addAll( fastaFiles );
        uploadFileList.addAll( flatFiles );
        uploadFileList.addAll( agpFiles );
        uploadFileList.addAll( tsvFiles );
       
        return uploadFileList;
    }
    
    
    protected List<Element>
    getXMLFiles( Path uploadDir ) throws IOException
    {
        List<Element> eList = new ArrayList<>();

        if( null != chromosomeListFile )
            eList.add( createfileElement( uploadDir, chromosomeListFile, "chromosome_list" ) );
        
        if( null != unlocalisedListFile )
            eList.add( createfileElement( uploadDir, unlocalisedListFile, "unlocalised_list" ) );
        
        fastaFiles.forEach( file -> eList.add( createfileElement( uploadDir, file, "fasta" ) ) );
        flatFiles.forEach( file -> eList.add( createfileElement( uploadDir, file, "flatfile" ) ) );
        agpFiles.forEach( file -> eList.add( createfileElement( uploadDir, file, "agp" ) ) );
        tsvFiles.forEach( file -> eList.add( createfileElement( uploadDir, file, "tab" ) ) );

        return eList;
    }
    
    
    @Override public void
    prepareSubmissionBundle() throws WebinCliException
    {
        try
        {
            Path uploadDir = Paths.get( String.valueOf( getContext() ), getName() );
            List<File> uploadFileList = getUploadFiles();
            List<Element> eList = getXMLFiles( uploadDir );

            String xml = createAnalysisXml( eList, getAssemblyInfo(), getParameters().getCenterName() );
            
            Path analysisFile = getSubmitDir().toPath().resolve( ANALYSIS_XML );
    
            Files.write( analysisFile, xml.getBytes( StandardCharsets.UTF_8 ), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC );
    
            setSubmissionBundle( new SubmissionBundle( getSubmitDir(), 
                                                       uploadDir.toString(), 
                                                       uploadFileList, 
                                                       Arrays.asList( new SubmissionXMLFile( SubmissionXMLFileType.ANALYSIS, analysisFile.toFile(), FileUtils.calculateDigest( "MD5", analysisFile.toFile() ) ) ), 
                                                       getParameters().getCenterName() ) );   
        } catch( IOException | NoSuchAlgorithmException e )
        {
            throw WebinCliException.createSystemError( e.getMessage() );
        }        
    }
}
