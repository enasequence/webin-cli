package uk.ac.ebi.ena.rawreads;

import java.io.BufferedInputStream;
import java.io.File;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import htsjdk.samtools.DefaultSAMRecordFactory;
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
import uk.ac.ebi.ena.frankenstein.loader.common.QualityNormalizer;
import uk.ac.ebi.ena.frankenstein.loader.common.eater.DataEaterException;
import uk.ac.ebi.ena.frankenstein.loader.common.eater.NullDataEater;
import uk.ac.ebi.ena.frankenstein.loader.common.feeder.AbstractDataFeeder;
import uk.ac.ebi.ena.frankenstein.loader.common.feeder.DataFeederException;
import uk.ac.ebi.ena.frankenstein.loader.fastq.DataSpot;
import uk.ac.ebi.ena.frankenstein.loader.fastq.DataSpot.DataSpotParams;
import uk.ac.ebi.ena.rawreads.RawReadsFile.ChecksumMethod;
import uk.ac.ebi.ena.rawreads.RawReadsFile.Filetype;
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

public class 
RawReadsWebinCli extends AbstractWebinCli
{   
    public enum Platforms { ILLUMINA, LS454, SOLID, COMPLETE_GENOMICS, HELICOS, PACBIO, IONTORRENT, CAPILLARY }
    private static final String RUN_XML = "run.xml";
    private static final String EXPERIMENT_XML = "experiment.xml";
    private static final String BAM_STAR = "*";
    RawReadsManifest rrm = new RawReadsManifest();
    private String  experiment_ref;
    private boolean valid;
    private File    submit_dir;
    private File    validate_dir;
    //TODO value should be estimated via validation
    private boolean is_paired;
    private boolean verify_sample = true;
    private boolean verify_study  = true;
    
    
    @Override public void 
    init( WebinCliParameters parameters ) throws ValidationEngineException
    {
        try
        {
            super.init( parameters );

            defineFileTypes( getParameters().getManifestFile() );
//            setAssemblyInfo( defineInfo( infoFile ) );
            setName( rrm.getName().trim().replaceAll( "\\s+", "_" ) );
            
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
    

    public boolean 
    getVerifyStudy()
    {
        return verify_study;
    }


    public boolean 
    getVerifySample()
    {
        return verify_sample;
    }


    public void 
    setVerifySample( boolean verify_sample )
    {
        this.verify_sample = verify_sample;
    }
    
    
    public void 
    setVerifyStudy( boolean verify_study )
    {
        this.verify_study = verify_study;
    }

    
    void
    defineFileTypes( File manifest_file ) throws IOException
    {
        rrm.defineFileTypes( getParameters().getInputDir().toPath(), manifest_file );
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


    DataFeederException 
    read( InputStream is, String stream_name, final QualityNormalizer normalizer, AtomicLong reads_cnt, Set<String> names, Set<String>labels ) throws SecurityException, DataFeederException, NoSuchMethodException, IOException, InterruptedException
    {
        AbstractDataFeeder<DataSpot> df = new AbstractDataFeeder<DataSpot>( is, DataSpot.class ) 
        {
            DataSpotParams params = DataSpot.defaultParams();
            
            @Override protected DataSpot 
            newFeedable()
            {
                return new DataSpot( normalizer, "", params );
            }
        };
        
        df.setName( stream_name );
        
        df.setEater( new NullDataEater<DataSpot>() 
        {
            @Override public void
            eat( DataSpot spot ) throws DataEaterException
            {
                int slash_idx = spot.bname.lastIndexOf( '/' );
                String name = slash_idx == -1 ? spot.bname 
                                              : spot.bname.substring( 0, slash_idx );
                String label = slash_idx == -1 ? stream_name
                                               : spot.bname.substring( slash_idx + 1 );
                
                if( reads_cnt.incrementAndGet() <= rrm.getPairingHorizon() )
                    names.add( name );
                
                if( labels.size() < rrm.getPairingHorizon() )
                    labels.add( label );
            }  
        } );
        
        df.start();
        df.join();
        return df.isOk() ? df.getFieldFeedCount() > 0 ? null : new DataFeederException( 0, "Empty file" ) : (DataFeederException)df.getStoredException().getCause(); 
    }
    

    @Override public boolean
    validate() throws ValidationEngineException
    {
        if( !FileUtils.emptyDirectory( validate_dir ) )
            throw WebinCliException.createSystemError( "Unable to empty directory " + validate_dir );
        
        if( !FileUtils.emptyDirectory( submit_dir ) )
            throw WebinCliException.createSystemError( "Unable to empty directory " + submit_dir );
        
        if( getVerifySample() )
            Sample.getSample( rrm.getSampleId(), getParameters().getUsername(), getParameters().getPassword(), getTestMode() );
        
        if( getVerifyStudy() )
            Study.getStudy( rrm.getStudyId(), getParameters().getUsername(), getParameters().getPassword(), getTestMode() );
        
        boolean valid = true;
        AtomicBoolean paired = new AtomicBoolean();
        
        List<RawReadsFile> files = rrm.getFiles();
        
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
    
    
    InputStream 
    openFileInputStream( Path path )
    {
        final int marksize = 256;
        BufferedInputStream is = null;
        try 
        {
            is = new BufferedInputStream( Files.newInputStream( path ) );
            is.mark( marksize );
            try
            {
                return new BufferedInputStream( new GZIPInputStream( is ) );
            } catch( IOException gzip )
            {
                is.reset();
                try
                {
                    is.mark( marksize );
                    return new BufferedInputStream( new BZip2CompressorInputStream( is ) );
                } catch( IOException bzip )
                {
                    is.reset();
                    return is;
                }
            }
        } catch( IOException ioe )
        {
            throw WebinCliException.createSystemError( ioe.getMessage() );
        }
    }
    
    
    private boolean
    readFastqFile( List<RawReadsFile> files, AtomicBoolean paired )
    {
        boolean valid = true;
        ValidationResult vr = new ValidationResult();
        QualityNormalizer qn = QualityNormalizer.NONE;
        List<Set<String>> names = new ArrayList<>( files.size() );
        List<Set<String>> labels = new ArrayList<>( files.size() );
        
        for( RawReadsFile rf : files )
        {
            File reportFile = getReportFile( String.valueOf( rf.getFiletype() ), rf.getFilename() );
            try( InputStream is = openFileInputStream( Paths.get( rf.getFilename() ) ); )
            {
                if( null != rf.getQualityScoringSystem() )
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
                }
                AtomicLong reads_cnt = new AtomicLong();
                Set<String> nameset  = new HashSet<>( rrm.getPairingHorizon() );
                Set<String> labelset = new HashSet<>( rrm.getPairingHorizon() );
                
                names.add( nameset );
                labels.add( labelset );
                
                DataFeederException t = read( is, rf.getFilename(), qn, reads_cnt, nameset, labelset );
                                
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
                FileUtils.writeReport( reportFile, Severity.INFO, "Collected " + labelset.size() +" read pair label(s): " + ( labelset.size() < 10 ? labelset : "" ) );
            } catch( Exception e )
            {
                throw WebinCliException.createSystemError( "Unable to validate file: " + rf + ", " + e.getMessage() );
            }
        }
        
        //check paired
        if( valid )
        {
            if( 2 == files.size() )
            {
                Set<String> n0 = names.get( 0 );
                Set<String> n1 = names.get( 1 );
                int max_size = Math.max( n0.size(), n1.size() );
                int resulted_size = max_size;
                if( n0.size() > n1.size() )
                {
                    n0.removeAll( n1 );
                    resulted_size = n0.size();
                } else
                {
                    n1.removeAll( n0 );
                    resulted_size = n1.size();
                }
                
                if( resulted_size >= ( max_size / 2 ) )
                {
                    String msg = "Fastq files from different runs are submitted: " + files;
                    reportToFileList( files, msg );
                    throw WebinCliException.createValidationError( "Fastq files from different runs are submitted: " + files );
                }
                
                if( labels.get( 0 ).containsAll( labels.get( 1 ) ) 
                 && labels.get( 1 ).containsAll( labels.get( 0 ) ) )
                {
                    valid = false;
                    String msg = "Same fastq files are submitted: " + files;
                    reportToFileList( files, msg );
                    throw WebinCliException.createValidationError( "Same fastq files are submitted: " + files );
                }
                
                for( int index = 0; index < files.size(); ++ index )
                {
                    if( getPaired( labels.get( index ) ) )
                    {
                        valid = false;
                        String msg = "Paired fastq file should be single file in submission: " + files.get( index ); 
                        reportToFileList( files, msg );
                        throw WebinCliException.createValidationError( "Paired fastq file should be single file in submission: " + files.get( index ) );
                    }
                }
                
                paired.set( true );
                
            } else if( 1 == files.size() )
            {
                paired.set( getPaired( labels.get( 0 ) ) );
            } else
            {
                valid = false;
                String msg = "Unable to validate unusual amount of files: " + files;
                reportToFileList( files, msg );
                throw WebinCliException.createValidationError( "Unable to validate unusual amount of files: " + files ); 
            }
        }
        return valid;
    }


    private void reportToFileList( List<RawReadsFile> files, String msg )
    {
        for( RawReadsFile rf : files )
        {
            File reportFile = getReportFile( String.valueOf( rf.getFiletype() ), rf.getFilename() );
            FileUtils.writeReport( reportFile, Severity.ERROR, msg );
        }
    }


    
    
    private boolean
    getPaired( Set<String> labelset )
    {
        if( 1 == labelset.size() )
        {
            return false;
        } else if( 2 == labelset.size() )
        {
            return true;
        } else
        {
            throw WebinCliException.createValidationError( "Unable to determine pairing from set: " + labelset.stream().limit( 10 ).collect( Collectors.joining( ",", "", 10 < labelset.size() ? "..." : "" ) ) );    
        }

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
                factory.enable( SamReaderFactory.Option.DONT_MEMORY_MAP_INDEX );
                factory.validationStringency( ValidationStringency.SILENT );
                factory.referenceSource( new ReferenceSource( (File) null ) );
                factory.samRecordFactory( DefaultSAMRecordFactory.getInstance() );
                SamInputResource ir = SamInputResource.of( file );
                File indexMaybe = SamFiles.findIndex( file );
                FileUtils.writeReport( reportFile, Severity.INFO, "proposed index: " + indexMaybe );
                
                if( null!= indexMaybe )
                    ir.index( indexMaybe );
                
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
            List<RawReadsFile> files = rrm.getFiles().stream().filter( e -> !Filetype.info.equals( e.getFiletype() ) ).collect( Collectors.toList() );
            
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
            
            String e_xml = createExperimentXml( experiment_ref, getParameters().getCenterName(), rrm, is_paired );
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
    createExperimentXml( String experiment_ref, String centerName, RawReadsManifest rrm, boolean is_paired ) 
    {
        String instrument_model = rrm.getInstrument();
        String design_description = "unspecified";
        String library_strategy  = rrm.getLibraryStrategy();
        String library_source    = rrm.getLibrarySource();
        String library_selection = rrm.getLibrarySelection();
        String sample_id = rrm.getSampleId();
        String study_id  = rrm.getStudyId();
        String platform  = rrm.getPlatform();
        Integer insert_size = rrm.getInsertSize();
                
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
            if( !is_paired )
            {
                libraryLayoutE.addContent( new Element( "SINGLE" ) );
            } else
            {
                Element pairedE = new Element( "PAIRED" );
                libraryLayoutE.addContent( pairedE );
                
                if( null != insert_size )
                    pairedE.setAttribute( "NOMINAL_LENGTH", String.valueOf( insert_size ) );
            }

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
