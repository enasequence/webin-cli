package uk.ac.ebi.ena.rawreads;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import uk.ac.ebi.ena.rawreads.RawReadsFile.AsciiOffset;
import uk.ac.ebi.ena.rawreads.RawReadsFile.Compression;
import uk.ac.ebi.ena.rawreads.RawReadsFile.Filetype;
import uk.ac.ebi.ena.rawreads.RawReadsFile.QualityScoringSystem;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class
RawReadsManifest
{
    public interface 
    RawReadsManifestTags
    {
        String LIBRARY_SELECTION = "LIBRARY_SELECTION";
        String LIBRARY_SOURCE    = "LIBRARY_SOURCE";
        String LIBRARY_STRATEGY  = "LIBRARY_STRATEGY";
        String PLATFORM = "PLATFORM";
        String NAME = "NAME";
        String STUDY = "STUDY";
        String SAMPLE = "SAMPLE";
        String INSERT_SIZE = "INSERT_SIZE";
        String LIBRARY_CONSTRUCTION_PROTOCOL = "LIBRARY_CONSTRUCTION_PROTOCOL";
        String INSTRUMENT = "INSTRUMENT";
        String LIBRARY_NAME = "LIBRARY_NAME";
        
    }


    public String 
    getStudyId()
    {
        return study_id;
    }


    public String 
    getSampleId()
    {
        return sample_id;
    }


    public String 
    getName()
    {
        return name;
    }


    public String 
    getPlatform()
    {
        return platform;
    }

    
    public List<RawReadsFile> 
    getFiles()
    {
        return files;
    }


    public Integer 
    getInsertSize()
    {
        return insert_size;
    }


    public String 
    getLibraryConstructionProtocol()
    {
        return library_construction_protocol;
    }


    public String 
    getLibraryName()
    {
        return library_name;
    }


    public String 
    getInstrument()
    {
        return instrument;
    }


    public String 
    getLibrarySource()
    {
        return library_source;
    }


    public String 
    getLibrarySelection()
    {
        return library_selection;
    }


    public String 
    getLibraryStrategy()
    {
        return library_strategy;
    }


    private String study_id = null;
    private String sample_id = null;
    private String name = null;
    private String platform = null;
    private List<RawReadsFile> files;
    private Integer insert_size;
    private String library_construction_protocol;
    private String library_name;
    private String instrument;
    private String library_source;
    private String library_selection;
    private String library_strategy;


    
    RawReadsFile
    parseFileLine( Path input_dir, String[] tokens )
    {
        RawReadsFile result = new RawReadsFile();
        result.setInputDir( input_dir );
        result.setCompression( Compression.NONE );
        
        for( String token : tokens )
        {
            token = token.trim();
            switch( token )
            {
            case "INFO":
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
    
    
    private static List<String>
    readAllLines( InputStream is )
    {
        return new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) ).lines().collect( Collectors.toList() ); 
    }
    
    
    public static List<String>
    readAllLines( File file ) throws FileNotFoundException
    {
        try( InputStream is = new GZIPInputStream( new FileInputStream( file ) ) )
        {
            return readAllLines( is ); 
        }catch( IOException ioe )
        {
            try( InputStream is = new BZip2CompressorInputStream( new FileInputStream( file ) ) )
            {
                return readAllLines( is );
            }catch( IOException ie )
            {
                return readAllLines( new FileInputStream( file ) );
            }
        }
    }
    
    
    void
    defineFileTypes( Path input_dir, File manifest_file ) throws IOException
    {
        List<String> lines = Files.readAllLines( manifest_file.toPath() );
        String source = manifest_file.getPath();

        this.files = parseContent( input_dir, lines, source );
        RawReadsFile info = files.stream().filter( e -> Filetype.info.equals( e.getFiletype() ) ).findFirst().orElse( null );
        if( null != info )
            parseContent( input_dir, readAllLines( new File( info.getFilename() ) ), info.getFilename() );
        
        
        if( null == study_id || study_id.isEmpty() )
            throw WebinCliException.createUserError( "Study should be defined" );
        
        if( null == sample_id || sample_id.isEmpty() )
            throw WebinCliException.createUserError( "Sample should be defined" );
        
        if( null == platform && null == instrument )
            throw WebinCliException.createUserError( "Platform or/and instrument should be defined. Available platforms: " + ControlledValueList.Platform.keyList() + "; available instruments: " + ControlledValueList.Instrument.keyList() );

        if( null != instrument )
        {
            String[] platforms = ControlledValueList.Instrument.getValue( instrument ).split( "[;,]" );
            if( 1 == platforms.length )
            {
                platform = ControlledValueList.Platform.getKey( platforms[ 0 ] );
            } else if( !Stream.of( platforms ).anyMatch( e -> e.equals( platform ) ) )
            {
                throw WebinCliException.createUserError( String.format( "Platform %s. Available platforms for instrument %s: %s", 
                                                                        null == platform || platform.isEmpty() ? "not defined" : platform + " not supported", 
                                                                        instrument, 
                                                                        ControlledValueList.Instrument.getValue( instrument ) ) );    
            }
        } else
        {
            instrument = ControlledValueList.Instrument.getKey( "unspecified" );
            if( null == instrument || instrument.isEmpty() )
                throw WebinCliException.createSystemError( "Instrument unspecified value is missing" ); 
        }
        
        reportIfManatoryValueMissing( library_strategy,  RawReadsManifestTags.LIBRARY_STRATEGY,  ControlledValueList.Strategy.keyList() );
        reportIfManatoryValueMissing( library_source,    RawReadsManifestTags.LIBRARY_SOURCE,    ControlledValueList.Source.keyList() );
        reportIfManatoryValueMissing( library_selection, RawReadsManifestTags.LIBRARY_SELECTION, ControlledValueList.Selection.keyList() );
        
        List<RawReadsFile> files = this.files.stream().filter( e -> !Filetype.info.equals( e.getFiletype() ) ).collect( Collectors.toList() );
        
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
    }


    private List<RawReadsFile> 
    parseContent( Path input_dir, List<String> lines, String source )
    {
        List<RawReadsFile> files = new ArrayList<>();
        int line_no = 0;
        
        for( String line : lines )
        {
            ++line_no;
            if( null != line && ( line = line.trim() ).isEmpty() )
                continue;
            
            String tokens[] = line.split( "\\s+", 2 );

            if( 0 == tokens.length )
                continue;

            String token0 = tokens[ 0 ].trim();
            if( null != token0 && token0.matches( "^[\\s]*(#|;|\\/\\/).*$" ) )
                continue;
                
            switch( token0 )
            {
            //paired with PLATFORM
            case RawReadsManifestTags.INSTRUMENT:
                if( null == instrument )
                {
                    if( !ControlledValueList.Instrument.contains( tokens[ 1 ] ) )
                        reportTokenValueConstrains( source, line_no, token0, ControlledValueList.Instrument.keyList() );
                    
                    instrument = ControlledValueList.Instrument.getKey( tokens[ 1 ] );
                    break;
                } 
                reportTokenDuplication( source, line_no, token0 );
                
            //optional, free text
            case RawReadsManifestTags.LIBRARY_NAME:
                if( null == library_name )
                {
                    library_name = tokens[ 1 ];
                    break;
                } 
                reportTokenDuplication( source, line_no, token0 );
                
            case RawReadsManifestTags.LIBRARY_SOURCE:
                if( null == library_source )
                {
                    if( !ControlledValueList.Source.contains( tokens[ 1 ] ) )
                        reportTokenValueConstrains( source, line_no, token0, ControlledValueList.Source.keyList() );
                    
                    library_source = ControlledValueList.Source.getKey( tokens[ 1 ] );
                    break;
                } 
                reportTokenDuplication( source, line_no, token0 );
                
            case RawReadsManifestTags.LIBRARY_SELECTION:
                if( null == library_selection )
                {
                    if( !ControlledValueList.Selection.contains( tokens[ 1 ] ) )
                        reportTokenValueConstrains( source, line_no, token0, ControlledValueList.Selection.keyList() );
                    
                    library_selection = ControlledValueList.Selection.getKey( tokens[ 1 ] );
                    break;
                } 
                reportTokenDuplication( source, line_no, token0 );
                

            case RawReadsManifestTags.LIBRARY_STRATEGY:
                if( null == library_strategy )
                {
                    if( !ControlledValueList.Strategy.contains( tokens[ 1 ] ) )
                        reportTokenValueConstrains( source, line_no, token0, ControlledValueList.Strategy.keyList() );
                    
                    library_strategy = ControlledValueList.Strategy.getKey( tokens[ 1 ] );
                    break;
                } 
                reportTokenDuplication( source, line_no, token0 );

            //optional, free text
            case RawReadsManifestTags.LIBRARY_CONSTRUCTION_PROTOCOL:
                if( null == library_construction_protocol )
                {
                    library_construction_protocol = tokens[ 1 ];
                    break;
                } 
                reportTokenDuplication( source, line_no, token0 );
                
            case RawReadsManifestTags.INSERT_SIZE:
                if( null == insert_size )
                {
                    try
                    {
                        insert_size = Integer.valueOf( tokens[ 1 ] );
                        if( insert_size < 0 )
                            reportUserError( source, line_no, "Non-negative integer expected" );
                        
                        break;
                    } catch( NumberFormatException nfe )
                    {
                        reportUserError( source, line_no, "Non-negative integer expected" );
                    }
                } 
                reportTokenDuplication( source, line_no, token0 );
            
            case RawReadsManifestTags.SAMPLE:
//            case "SAMPLE-ID":
//            case "SAMPLE_ID":
                if( null == sample_id )
                {
                    sample_id = tokens[ 1 ];
                    break;
                } 
                reportTokenDuplication( source, line_no, token0 );

            case RawReadsManifestTags.STUDY:
//            case "STUDY-ID":
//            case "STUDY_ID":
                if( null == study_id )
                {
                    study_id = tokens[ 1 ];
                    break;
                } 
                reportTokenDuplication( source, line_no, token0 );
                
            case RawReadsManifestTags.NAME:
                if( null == name )
                {
                    name = tokens[ 1 ];
                    break;
                } 
                reportTokenDuplication( source, line_no, token0 );

            case RawReadsManifestTags.PLATFORM:
                if( null == platform )
                {
                    if( !ControlledValueList.Platform.contains( tokens[ 1 ] ) )
                        reportTokenValueConstrains( source, line_no, token0, ControlledValueList.Platform.keyList() );
                    platform = ControlledValueList.Platform.getKey( tokens[ 1 ] );
                    break;
                } 
                reportTokenDuplication( source, line_no, token0 );

            default:
                RawReadsFile f = parseFileLine( input_dir, line.split( "\\s+" ) );
                if( null == f.getFilename() || f.getFilename().isEmpty() )
                    reportUserError( source, line_no, "Filename not supplied" );
                
                if( null == f.getFiletype() )
                    reportUserError( source, line_no, "No file type supplied. Valid are: " + Stream.of( Filetype.values() ).map( String::valueOf ).collect( Collectors.joining( ", " ) ) );

                if( Compression.NONE != f.getCompression() && ( Filetype.bam == f.getFiletype() || Filetype.cram == f.getFiletype() ) )
                    reportUserError( source, line_no, "Compression not supported for types " + Filetype.bam + " and " + Filetype.cram );
                
                if( null != f.getQualityScoringSystem() && ( Filetype.bam == f.getFiletype() || Filetype.cram == f.getFiletype() ) )
                    reportUserError( source, line_no, "Scoring system not supported for types " + Filetype.bam + " and " + Filetype.cram );

                //TODO externalise scoring types
                if( Filetype.fastq == f.getFiletype() && null == f.getQualityScoringSystem() )
                    reportUserError( source, line_no, "No scoring system supplied for fastq file. Valid are: PHRED_33, PHRED_64, LOGODDS" );
                
                if( !Files.exists( Paths.get( f.getFilename() ) ) )
                    reportUserError( source, line_no, String.format( "Cannot locate file %s", f.getFilename() ) );
                
                if( Files.isDirectory( Paths.get( f.getFilename() ) ) )
                    reportUserError( source, line_no, "Supplied file is a directory " + f.getFilename() );
                
                files.add( f );
                break;
            }
        }
        return files;
    }


    private void 
    reportTokenDuplication( String source, int line_no, String token )
    {
        throw WebinCliException.createUserError( String.format( "%s: %d, %s should not appeared more than once", source, line_no, token ) );
    }

    
    private void 
    reportTokenValueConstrains( String source, int line_no, String token, List<String> values )
    {
        throw WebinCliException.createUserError( String.format( "%s: %d, Value of %s should be one from the list %s", source, line_no, token, values ) );
    }
    

    private void 
    reportUserError( String source, int line_no, String error )
    {
        throw WebinCliException.createUserError( String.format( "%s: %d, %s", source, line_no, error ) );
    }
    
    //TODO maybe merge with reportTokenValueConstrains
    private void
    reportIfManatoryValueMissing( String value, String tag_name, List<String> value_list )
    {
        if( null == value || value.isEmpty() )
            throw WebinCliException.createUserError( String.format( "%s is mandatory.%s", 
                                                                    tag_name, 
                                                                    null != value_list ? " Possible values: " + value_list.stream().collect( Collectors.joining( ", " ) ) : "" ) );
    }


}
