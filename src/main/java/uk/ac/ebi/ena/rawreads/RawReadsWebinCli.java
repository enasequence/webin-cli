package uk.ac.ebi.ena.rawreads;

import java.io.BufferedInputStream;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import htsjdk.samtools.SAMFormatException;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamFiles;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.cram.ref.ReferenceSource;
import htsjdk.samtools.util.Log;
import htsjdk.samtools.util.Log.LogLevel;
import uk.ac.ebi.embl.api.validation.DefaultOrigin;
import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.frankenstein.loader.common.FileCompression;
import uk.ac.ebi.ena.frankenstein.loader.common.QualityNormalizer;
import uk.ac.ebi.ena.frankenstein.loader.common.eater.DataEaterException;
import uk.ac.ebi.ena.frankenstein.loader.common.eater.NullDataEater;
import uk.ac.ebi.ena.frankenstein.loader.common.feeder.AbstractDataFeeder;
import uk.ac.ebi.ena.frankenstein.loader.common.feeder.DataFeederException;
import uk.ac.ebi.ena.frankenstein.loader.fastq.DataSpot;
import uk.ac.ebi.ena.frankenstein.loader.fastq.DataSpot.DataSpotParams;
import uk.ac.ebi.ena.rawreads.RawReadsFile.AsciiOffset;
import uk.ac.ebi.ena.rawreads.RawReadsFile.ChecksumMethod;
import uk.ac.ebi.ena.rawreads.RawReadsFile.Compression;
import uk.ac.ebi.ena.rawreads.RawReadsFile.Filetype;
import uk.ac.ebi.ena.rawreads.RawReadsFile.QualityScoringSystem;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.submit.SubmissionBundle;
import uk.ac.ebi.ena.submit.SubmissionBundle.SubmissionXMLFile;
import uk.ac.ebi.ena.submit.SubmissionBundle.SubmissionXMLFileType;
import uk.ac.ebi.ena.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.AbstractWebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

public class 
RawReadsWebinCli extends AbstractWebinCli
{   
    public enum Platforms { ILLUMINA, LS454, SOLID, COMPLETE_GENOMICS, HELICOS, PACBIO, IONTORRENT, CAPILLARY }
    private static final String RUN_XML = "run.xml";
    private static final String EXPERIMENT_XML = "experiment.xml";
    private static final String BAM_STAR = "*";
    List<RawReadsFile> files;
    private String  experiment_ref;
    private boolean valid;
    private boolean test_mode;
    private File    submit_dir;
    private File    validate_dir;
    private String study_id;
    private String sample_id;
    private String platform;
    //TODO value should be estimated via validation
    private boolean is_paired;
    
    
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
        result.setCompression( Compression.NONE );
        
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
            
            case "NONE":
            case "GZ":
            case "GZIP":
            case "BZ2":
                result.setCompression( Compression.valueOf( token ) );
                break;
                
            case "ZIP": //Do not support zip
                result.setCompression( Compression.NONE );
                break;
            
            default:
                if( null != token && !token.isEmpty() )
                    result.setFilename( token );
            }
        }
        
        if( null != result.getFilename() && !result.getFilename().isEmpty() && !Paths.get( result.getFilename() ).isAbsolute() )
            result.setFilename( result.getInputDir().resolve( Paths.get( result.getFilename() ) ).toString() );
        
        return result;
    }
    
    
    void
    defineFileTypes( File manifest_file ) throws IOException
    {
        List<String> lines = Files.readAllLines( manifest_file.toPath() );
        List<RawReadsFile> files = new ArrayList<>();
        
        String study_id = null;
        String sample_id = null;
        String name = null;
        String platform = null;
        int line_no = 0;
        
        for( String line : lines )
        {
            ++line_no;
            if( null != line && ( line = line.trim() ).isEmpty() )
                continue;
            
            String tokens[] = line.split( "\\s+" );

            if( 0 == tokens.length )
                continue;

            String token0 = tokens[ 0 ].trim();
            if( null != token0 && token0.matches( "^[\\s]*(#|;|\\/\\/).*$" ) )
                continue;
                
            switch( token0 )
            {
            case "SAMPLE":
            case "SAMPLE-ID":
            case "SAMPLE_ID":
                if( null == sample_id )
                {
                    sample_id = tokens[ 1 ];
                    break;
                } 
                throw WebinCliException.createUserError( String.format( "Line: %d, %s", line_no, "Sample should not appeared more than once" ) );

            case "STUDY":
            case "STUDY-ID":
            case "STUDY_ID":
                if( null == study_id )
                {
                    study_id = tokens[ 1 ];
                    break;
                } 
                throw WebinCliException.createUserError( String.format( "Line: %d, %s", line_no, "Study should not appeared more than once" ) );
                
            case "NAME":
                if( null == name )
                {
                    name = tokens[ 1 ];
                    break;
                } 
                throw WebinCliException.createUserError( String.format( "Line: %d, %s", line_no, "Name should not appeared more than once" ) );

            case "PLATFORM":
                if( null == platform )
                {
                    platform = tokens[ 1 ];
                    break;
                } 
                throw WebinCliException.createUserError( String.format( "Line: %d, %s", line_no, "Platform should not appeared more than once" ) );

            default:
                RawReadsFile f = parseFileLine( tokens );
                if( null == f.getFilename() || f.getFilename().isEmpty() ) 
                    throw WebinCliException.createUserError( String.format( "Line: %d, %s", line_no, "Filename not supplied" ) );
                
                if( null == f.getFiletype() )
                    throw WebinCliException.createUserError( String.format( "Line: %d, %s", line_no, "No file type supplied. Valid are: " + Stream.of( Filetype.values() ).map( String::valueOf ).collect( Collectors.joining( ", " ) ) ) );

                if( Compression.NONE != f.getCompression() && ( Filetype.bam == f.getFiletype() || Filetype.cram == f.getFiletype() ) )
                    throw WebinCliException.createUserError( String.format( "Line: %d, %s", line_no, "Compression not supported for types " + Filetype.bam + " and " + Filetype.cram ) );
                
                if( null != f.getQualityScoringSystem() && ( Filetype.bam == f.getFiletype() || Filetype.cram == f.getFiletype() ) )
                    throw WebinCliException.createUserError( String.format( "Line: %d, %s", line_no, "Scoring system not supported for types " + Filetype.bam + " and " + Filetype.cram ) );

                //TODO externalise scoring types
                if( Filetype.fastq == f.getFiletype() && null == f.getQualityScoringSystem() )
                    throw WebinCliException.createUserError( String.format( "Line: %d, %s", line_no, "No scoring system supplied for fastq file. Valid are: PHRED_33, PHRED_64, LOGODDS" ) );
                
                if( !Files.exists( Paths.get( f.getFilename() ) ) )
                    throw WebinCliException.createUserError( String.format( "Line: %d, %s", line_no, String.format( "Cannot locate file %s", f.getFilename() ) ) );
                
                if( Files.isDirectory( Paths.get( f.getFilename() ) ) )
                    throw WebinCliException.createUserError( String.format( "Line: %d, %s", line_no, String.format( "Supplied file is a directory %s", f.getFilename() ) ) );
                
                files.add( f );
                break;
            }
        }
        
        if( null == study_id || study_id.isEmpty() )
            throw WebinCliException.createUserError( "Study should be defined" );
        
        if( null == sample_id || sample_id.isEmpty() )
            throw WebinCliException.createUserError( "Sample should be defined" );
        
        if( null == name || name.isEmpty() )
            setName( String.format( "%s-%s", "WEBIN-CLI-EXPERIMENT", System.currentTimeMillis() ) );
        else
            setName( name );
        
        if( null == platform )
            throw WebinCliException.createUserError( "Platform should be defined" );
        try
        {
            Platforms.valueOf( platform );
        } catch( Throwable t )
        {
            throw WebinCliException.createUserError( "Platform value should be one of " + Arrays.asList( Platforms.values() ) );
        }
        
        this.study_id = study_id;
        this.sample_id = sample_id;
        this.platform = platform;

        if( files.isEmpty() )
            throw WebinCliException.createUserError( "No files supplied" );
        
        if( 1 != files.stream().map( e -> e.getFiletype() ).collect( Collectors.toSet() ).size() )
            throw WebinCliException.createUserError( "Cannot mix following file formats in one manifest: " + Arrays.asList( files.stream().map( e -> e.getFiletype() ).collect( Collectors.toSet() ) ) );
        
        long cnt = files.stream().filter( e -> Filetype.fastq == e.getFiletype() ).collect( Collectors.counting() );
        
        if( 0 != cnt && 1 != cnt && 2 != cnt )
            throw WebinCliException.createUserError( "Amount of fastq files can be one for single or paired layout and two for paired layout" );
        
        cnt = files.stream().filter( e -> Filetype.bam == e.getFiletype() ).collect( Collectors.counting() );
        if( 0 != cnt && 1 != cnt )
            throw WebinCliException.createUserError( "Only one bam file accepted" );

        cnt = files.stream().filter( e -> Filetype.cram == e.getFiletype() ).collect( Collectors.counting() );
        if( 0 != cnt && 1 != cnt )
            throw WebinCliException.createUserError( "Only one cram file accepted" );

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

	

    DataFeederException 
    read( InputStream is, String name, final QualityNormalizer normalizer, AtomicLong reads_cnt ) throws SecurityException, DataFeederException, NoSuchMethodException, IOException, InterruptedException
    {
        AbstractDataFeeder<DataSpot> df = new AbstractDataFeeder<DataSpot>( is, DataSpot.class ) 
        {
//            final AtomicLong line_no = new AtomicLong( 1 );
//            final AtomicReference<DataSpot.ReadStyle> read_style = new AtomicReference<DataSpot.ReadStyle>();
            DataSpotParams params = DataSpot.defaultParams();
            
            @Override protected DataSpot 
            newFeedable()
            {
                return new DataSpot( normalizer, "", params );
            }
        };
        
        df.setName( name );
        df.setEater( new NullDataEater<DataSpot>() {
            @Override public void
            eat( DataSpot object ) throws DataEaterException
            {
                reads_cnt.incrementAndGet();
            }  } );
        df.start();
        df.join();
        return df.isOk() ? df.getFieldFeedCount() > 0 ? null : new DataFeederException( 0, "Empty file" ) : (DataFeederException)df.getStoredException().getCause(); 
    }
    
    
    Throwable 
    read( File file, final QualityNormalizer normalizer, AtomicLong reads_cnt ) throws Exception
    {
        InputStream is = new BufferedInputStream( new FileInputStream( file ) );
        try
        {
            return read( is, file.getPath(), normalizer, reads_cnt );
        } finally
        {
            is.close();
        }
    }


    @Override public boolean
    validate() throws ValidationEngineException
    {
        if( !FileUtils.emptyDirectory( validate_dir ) )
            throw WebinCliException.createSystemError( "Unable to empty directory " + validate_dir );
        
        if( !FileUtils.emptyDirectory( submit_dir ) )
            throw WebinCliException.createSystemError( "Unable to empty directory " + submit_dir );
        
        boolean valid = true;
        AtomicBoolean paired = new AtomicBoolean();
        
        for( RawReadsFile rf : files )
        {
            if( Filetype.fastq.equals( rf.getFiletype() ) )
            {
                valid &= readFastqFile( files, paired );
                
            } else if( Filetype.bam.equals( rf.getFiletype() ) )
            {
                valid &= readBamFile( files, paired );
                
            } else if( Filetype.cram.equals( rf.getFiletype() ) )
            {
                valid &= readCramFile( files, paired );

            } else
            {
                throw WebinCliException.createSystemError( "Filetype " + rf.getFiletype() + " is unknown" );
            }
            
            break;
        }  

        this.valid = valid;
        is_paired = paired.get();
        
        return valid;
    }

    
    private boolean
    readCramFile( List<RawReadsFile> files, AtomicBoolean paired ) throws ValidationEngineException
    {
        return readBamFile( files, paired );
    }
    
    
    private boolean
    readFastqFile( List<RawReadsFile> files, AtomicBoolean paired )
    {
        boolean valid = true;
        ValidationResult vr = new ValidationResult();
        QualityNormalizer qn = QualityNormalizer.NONE;

        for( RawReadsFile rf : files )
        {
            File reportFile = getReportFile( String.valueOf( rf.getFiletype() ), rf.getFilename() );
            try( InputStream is = FileCompression.valueOf( String.valueOf( rf.getCompression() ) ).open( rf.getFilename(), false ) )
            {
                switch( rf.getQualityScoringSystem() )
                {
                default:
                    throw WebinCliException.createSystemError( "Scoring system: " + String.valueOf( rf.getQualityScoringSystem() ) );
                    
                case phred:
                    switch( rf.getAsciiOffset() )
                    {
                    default:
                        throw WebinCliException.createSystemError( "ASCII offset: " + String.valueOf( rf.getAsciiOffset() ) );
                        
                    case FROM33:
                        qn = QualityNormalizer.X;
                        break;
                        
                    case FROM64:
                        qn = QualityNormalizer.X_2;
                        break;
                    }
                    break;
                    
                case log_odds:
                    qn = QualityNormalizer.SOLEXA;
                    break;
                    
                }
                
                AtomicLong reads_cnt = new AtomicLong();
                DataFeederException t = read( is, rf.getFilename(), qn, reads_cnt );
                                
                if( null != t )
                {
                    ValidationMessage<Origin> vm = new ValidationMessage<>( Severity.ERROR, ValidationMessage.NO_KEY, t.getMessage() );
                    vm.setThrowable( t );
                    vm.append( new DefaultOrigin( String.format( "%s:%d", rf.getFilename(), t.getLineNo() ) ) );
                    
                    valid = false;
                    vr.append( vm );
                }
    
                FileUtils.writeReport( reportFile, vr );
                FileUtils.writeReport( reportFile, Severity.INFO, "Valid reads count: " + reads_cnt.get() );
        
            } catch( Exception e )
            {
                throw WebinCliException.createSystemError( "Unable to validate file: " + rf + ", " + e.getMessage() );
            }
        }
        
        //TODO: implement properly
        paired.set( 2 == files.size() );
                
        return valid;
    }


    private boolean
    readBamFile( List<RawReadsFile> files, AtomicBoolean paired ) throws ValidationEngineException
    {
        boolean valid = true;
        for( RawReadsFile rf : files )
        {
            File reportFile = getReportFile( String.valueOf( rf.getFiletype() ), rf.getFilename() );
            long read_no = 0;
            long reads_cnt = 0;
            
            try
            {
                File file = new File( rf.getFilename() );
                Log.setGlobalLogLevel( LogLevel.ERROR );
                SamReaderFactory.setDefaultValidationStringency( ValidationStringency.SILENT );
                SamReaderFactory factory = SamReaderFactory.make();
                factory.referenceSource( new ReferenceSource( (File) null ) );
                SamInputResource ir = SamInputResource.of( file );
                File indexMaybe = SamFiles.findIndex( file );
                FileUtils.writeReport( reportFile, Severity.INFO, "proposed index: " + indexMaybe );
                SamReader reader = factory.open( ir );
                
                for( SAMRecord record : reader )
                {
                    read_no ++;
                    //do not load supplementary reads
                    if( record.isSecondaryOrSupplementary() )
                        continue;
                    
                    if( record.getDuplicateReadFlag() )
                        continue;
                    
                    if( record.getReadString().equals( BAM_STAR ) && record.getBaseQualityString().equals( BAM_STAR ) )
                        continue;
                    
                    if( record.getReadBases().length != record.getBaseQualities().length )
                    {
                        ValidationMessage<Origin> vm = new ValidationMessage<>( Severity.ERROR, ValidationMessage.NO_KEY, "Mismatch between length of read bases and qualities" );
                        vm.append( new DefaultOrigin( String.format( "%s:%d", rf.getFilename(), read_no ) ) );
                        
                        FileUtils.writeReport( reportFile, Arrays.asList( vm ) );
                        valid &= false;
                    }
                    
                    paired.compareAndSet( false, record.getReadPairedFlag() );
                    reads_cnt ++;
                }
        
                reader.close();
                
                FileUtils.writeReport( reportFile, Severity.INFO, "Valid reads count: " + reads_cnt );
                FileUtils.writeReport( reportFile, Severity.INFO, "LibraryLayout: " + ( paired.get() ? "PAIRED" : "SINGLE" ) );
                
                if( 0 == reads_cnt )
                {
                    FileUtils.writeReport( reportFile, Severity.ERROR, "File contains no valid reads" );
                    valid &= false;
                }
                
            } catch( SAMFormatException e )
            {
                FileUtils.writeReport( reportFile, Severity.ERROR, e.getMessage() );
                valid &= false;
                
            } catch( IOException e )
            {
                throw new ValidationEngineException( e );
            }
        }
        return valid;
    }


    @Override public SubmissionBundle
    getSubmissionBundle()
    {
        if( valid )
        {
            prepareSubmissionBundle();
        }
        return super.getSubmissionBundle();
    }


    public void
    prepareSubmissionBundle() throws WebinCliException
    {
        try
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
            String experiment_ref = String.format( "exp-%s", getName() );
            
            String e_xml = createExperimentXml( experiment_ref, getParameters().getCenterName(), study_id, sample_id, platform, is_paired );
            String r_xml = createRunXml( eList, experiment_ref, getParameters().getCenterName() );
            
            Path runXmlFile = getSubmitDir().toPath().resolve( RUN_XML );
            Path experimentXmlFile = getSubmitDir().toPath().resolve( EXPERIMENT_XML );
            
            Files.write( runXmlFile, r_xml.getBytes( StandardCharsets.UTF_8 ), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC );
            Files.write( experimentXmlFile, e_xml.getBytes( StandardCharsets.UTF_8 ), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC );
            
            setSubmissionBundle( new SubmissionBundle( getSubmitDir(), 
                                                       uploadDir.toString(), 
                                                       uploadFileList,
                                                       Arrays.asList( new SubmissionXMLFile( SubmissionXMLFileType.EXPERIMENT, experimentXmlFile.toFile(), FileUtils.calculateDigest( "MD5", experimentXmlFile.toFile() ) ), 
                                                                      new SubmissionXMLFile( SubmissionXMLFileType.RUN, runXmlFile.toFile(), FileUtils.calculateDigest( "MD5", runXmlFile.toFile() ) ) ),
                                                       getParameters().getCenterName() ) );
        } catch( NoSuchAlgorithmException | IOException e )
        {
            throw WebinCliException.createSystemError( e.getMessage() );
        }
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
    createExperimentXml( String experiment_ref, String centerName, String study_id, String sample_id, String platform, boolean is_paired ) 
    {
        String instrument_model = "unspecified";
        String design_description = "unspecified";
        String library_strategy  = "OTHER";
        String library_source    = "OTHER";
        String library_selection = "unspecified";
        
        try 
        {
            String full_name = ContextE.reads.getTitle( getName() );
            Element experimentSetE = new Element( "EXPERIMENT_SET" );
            Element experimentE = new Element( "EXPERIMENT" );
            experimentSetE.addContent( experimentE );
            
            Document doc = new Document( experimentSetE );
            experimentE.setAttribute( "alias", experiment_ref );
            
            if( null != centerName && !centerName.isEmpty() )
                experimentE.setAttribute( "center_name", centerName );
            
            experimentE.addContent( new Element( "TITLE" ).setText( full_name ) );
            
            Element studyRefE = new Element( "STUDY_REF" );
            experimentE.addContent( studyRefE );
            studyRefE.setAttribute( "accession", study_id );
  
            Element designE = new Element( "DESIGN" );
            experimentE.addContent( designE );
            
            Element designDescriptionE = new Element( "DESIGN_DESCRIPTION" );
            designDescriptionE.setText( design_description );
            designE.addContent( designDescriptionE );
            
            Element sampleDescriptorE = new Element( "SAMPLE_DESCRIPTOR" );
            sampleDescriptorE.setAttribute( "accession", sample_id );

            designE.addContent( sampleDescriptorE );

            Element libraryDescriptorE = new Element( "LIBRARY_DESCRIPTOR" );
            designE.addContent( libraryDescriptorE );
            
            Element libraryStrategyE = new Element( "LIBRARY_STRATEGY" );
            libraryStrategyE.setText( library_strategy );
            libraryDescriptorE.addContent( libraryStrategyE );
            
            Element librarySourceE = new Element( "LIBRARY_SOURCE" );
            librarySourceE.setText( library_source );
            libraryDescriptorE.addContent( librarySourceE );
            
            Element librarySelectionE = new Element( "LIBRARY_SELECTION" );
            librarySelectionE.setText( library_selection );
            libraryDescriptorE.addContent( librarySelectionE );

            Element libraryLayoutE = new Element( "LIBRARY_LAYOUT" );
            libraryLayoutE.addContent( new Element( is_paired ? "PAIRED" : "SINGLE" ) );
            libraryDescriptorE.addContent( libraryLayoutE );
            
            Element platformE = new Element( "PLATFORM" );
            experimentE.addContent( platformE );
            
            Element platformRefE = new Element( platform );
            platformE.addContent( platformRefE );
            Element instrumentModelE = new Element( "INSTRUMENT_MODEL" );
            instrumentModelE.setText( instrument_model );
            platformRefE.addContent( instrumentModelE );
            
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
    
    
    String
    createRunXml( List<Element> fileList, String experiment_ref, String centerName  ) 
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
            experimentRefE.setAttribute( "refname", experiment_ref );
            
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


    @Override public File
    getSubmissionBundleFileName()
    {
        return new File( submit_dir, "validate.reciept" );
    }


    @Override public File
    getValidationDir()
    {
        return validate_dir;
    }
}
