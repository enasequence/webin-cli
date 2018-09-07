package uk.ac.ebi.ena.rawreads;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import uk.ac.ebi.embl.api.validation.DefaultOrigin;
import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.frankenstein.loader.common.QualityNormalizer;
import uk.ac.ebi.ena.frankenstein.loader.common.eater.DataEaterException;
import uk.ac.ebi.ena.frankenstein.loader.common.eater.NullDataEater;
import uk.ac.ebi.ena.frankenstein.loader.common.feeder.AbstractDataFeeder;
import uk.ac.ebi.ena.frankenstein.loader.common.feeder.DataFeederException;
import uk.ac.ebi.ena.frankenstein.loader.fastq.DataSpot;
import uk.ac.ebi.ena.frankenstein.loader.fastq.DataSpot.DataSpotParams;
import uk.ac.ebi.ena.frankenstein.loader.fastq.IlluminaIterativeEater;
import uk.ac.ebi.ena.frankenstein.loader.fastq.IlluminaIterativeEater.READ_TYPE;
import uk.ac.ebi.ena.frankenstein.loader.fastq.IlluminaSpot;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class 
FastqScanner 
{
    private static final String S_READ_D = "%s, Read %d";
    private static final int BLOOM_EXPECTED_READS = 800_000_000;
    protected static final int MAX_LABEL_SET_SIZE = 10;
    
    private final int expected_size;
    private Set<String> labelset = new HashSet<>();
    private AtomicBoolean paired = new AtomicBoolean();
    
    
    public
    FastqScanner( int expected_size )
    {
        this.expected_size = expected_size;
    }

    
    public
    FastqScanner() 
    {
        this( BLOOM_EXPECTED_READS );
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
    
    
    private QualityNormalizer
    getQualityNormalizer( RawReadsFile rf )
    {
        QualityNormalizer qn = QualityNormalizer.NONE;
        
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
        return qn;
    }
    
    
    
    
    private DataFeederException 
    read( RawReadsFile rf,
          String       stream_name,
          Set<String>labels, 
          ReadNameSet<Void> pairing,
          ReadNameSet<String> duplications,
          AtomicLong count ) throws SecurityException, DataFeederException, NoSuchMethodException, IOException, InterruptedException
    {
        try( InputStream is = openFileInputStream( Paths.get( rf.getFilename() ) ); )
        {
            //String stream_name = rf.getFilename();
            final QualityNormalizer normalizer = getQualityNormalizer( rf );
            
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
                AtomicLong read_no = new AtomicLong( 1 );
                @Override public void
                eat( DataSpot spot ) throws DataEaterException
                {
                    int slash_idx = spot.bname.lastIndexOf( '/' );
                    String name = slash_idx == -1 ? spot.bname 
                                                  : spot.bname.substring( 0, slash_idx );
                    String label = slash_idx == -1 ? stream_name
                                                   : spot.bname.substring( slash_idx + 1 );
                    
                    if( labels.size() < MAX_LABEL_SET_SIZE )
                        labels.add( label );
                    
                    count.incrementAndGet();
                    pairing.add( name, null );
                    duplications.add( spot.bname, String.format( S_READ_D, stream_name, read_no.getAndIncrement() ) );
                    
                    if( 0 == count.get() % 1000 )
                        printProcessedReadNumber( count );
                }  
            } );

            System.out.println( "Processing file " + rf.getFilename() );
            //System.out.println();
            System.out.flush();
            df.start();
            df.join();
            DataFeederException result = df.isOk() ? df.getFieldFeedCount() > 0 ? null : new DataFeederException( 0, "Empty file" ) : (DataFeederException)df.getStoredException().getCause();
            printProcessedReadNumber( count );
            System.out.println();
            return result;
        }
    }


    private void 
    printProcessedReadNumber( AtomicLong count )
    {
        //System.out.printf( "\33[1A\33[2KProcessed %16d read(s)\n", count.get() );
        System.out.printf( "\rProcessed %16d read(s)", count.get() );
        System.out.flush();
    }

    
    private ValidationResult
    checkSingleFile( RawReadsFile rf, 
                     String stream_name, 
                     Set<String> labelset,
                     ReadNameSet<Void> pairing,
                     ReadNameSet<String> duplications ) throws SecurityException, NoSuchMethodException, DataFeederException, IOException, InterruptedException
    {
        ValidationResult vr = new ValidationResult();
        AtomicLong count = new AtomicLong();
        DataFeederException t = read( rf, stream_name, labelset, pairing, duplications, count );
                        
        if( null != t )
        {
            ValidationMessage<Origin> vm = fMsg( Severity.ERROR, t.getMessage(), new DefaultOrigin( String.format( "%s:%d", stream_name, t.getLineNo() ) ) );
            vm.setThrowable( t );
            vr.append( vm );
        } else
        {
            vr.append( fMsg( Severity.INFO, String.format( "%s: Collected %d reads", stream_name, count.get() ) ) );
            vr.append( fMsg( Severity.INFO, String.format( "%s: Collected %d read labels: %s", stream_name, labelset.size(), labelset ) ) );
            vr.append( fMsg( Severity.INFO, String.format( "%s: Has possible dublicate(s): " + duplications.hasPossibleDuplicates(), stream_name ) ) );
        }
        return vr;
    }

    
    private String
    formatStreamName( int index, String filename )
    {
        return String.format( "File[%d]: %s", index, filename );
    }
    
    
    public boolean 
    getPaired()
    {
        return this.paired.get();
    }
    
    
    ValidationMessage<Origin> 
    fMsg( Severity severity, String msg, Origin... origin )
    {
        ValidationMessage<Origin> result = new ValidationMessage<>( severity, ValidationMessage.NO_KEY );
        result.setMessage( msg );
        if( null != origin && origin.length > 0 )
            result.append( Arrays.asList( origin ) );
        return result;
    }
    
    
    public ValidationResult
    checkFiles( RawReadsFile... rfs ) throws SecurityException, NoSuchMethodException, DataFeederException, IOException, InterruptedException
    {
        ValidationResult vr = new ValidationResult();

        if( null == rfs || rfs.length != 1 && rfs.length != 2 )
        {
            //terminal error
            vr.append( fMsg( Severity.ERROR, "Unusual amount of files" ) );
            return vr;
        }   
        
        ReadNameSet<String> duplications = new ReadNameSet<>( expected_size );
        ReadNameSet<Void> pairing = new ReadNameSet<>( expected_size, false );
        
        int index = 1;
        for( RawReadsFile rf : rfs )
        {
            Set<String> flabelset = new HashSet<>();
            vr.append( checkSingleFile( rf, formatStreamName( index++, rf.getFilename() ), flabelset, pairing, duplications ) );
            labelset.addAll( flabelset );
        }
        
        if( 2 == labelset.size() )
        {
            paired.set( true );
            
            vr.append( fMsg( Severity.INFO, String.format( "Pairing percentage: %.2f%%", 100 * (double)pairing.getPossibleDuplicateCount() / (double)pairing.getAddsNumber() ) ) ); 
            
            //TODO: estimate bloom false positives impact
            if( (double)pairing.getPossibleDuplicateCount() < (double)pairing.getAddsNumber() / (double)3  )
            {
                //terminal error
                vr.append( fMsg( Severity.ERROR, "Detected paired fastq submission with less than 30% of paired reads" ) );
            }

        } else if( labelset.size() > 2 )
        {
            String msg = "When submitting paired reads using two Fastq files the reads must follow Illumina paired read naming conventions. "
                       + "This was not the case for the submitted Fastq files: ";

            vr.append( fMsg( Severity.ERROR, 
                             String.format( "%s%s. Unable to determine pairing from set: %s", 
                                            msg, 
                                            rfs,
                                            labelset.stream().limit( 10 ).collect( Collectors.joining( ",", "", 10 < labelset.size() ? "..." : "" ) ) ) ) ); 
        }   
        
        //extra check for suspected reads
        if( duplications.hasPossibleDuplicates() )
        {
            index = 1;
            Map<String, Set<String>> result = new LinkedHashMap<>();
            ValidationResult dvr = new ValidationResult();
            for( RawReadsFile rf : rfs )
            {
                String stream_name = formatStreamName( index++, rf.getFilename() );
                result.putAll( checkSuspected( rf, stream_name, duplications ) );
            }
                
                
            dvr.append( result.entrySet().stream().map( ( e ) -> fMsg( Severity.ERROR, 
                                                                      String.format( "Read %s has duplicate(s): %s", 
                                                                                     e.getKey(), 
                                                                                     e.getValue().toString() ) ) ).collect( Collectors.toList() ) );
            if( dvr.isValid() )
                dvr.append( fMsg( Severity.INFO, "No duplicates confirmed." ) );
            
            vr.append( dvr );
        }

        return vr;
    }

    
    private Map<String, Set<String>>
    checkSuspected( RawReadsFile rf,
                    String stream_name,
                    ReadNameSet<String> rns ) throws InterruptedException, SecurityException, NoSuchMethodException, DataFeederException, IOException
    {
        IlluminaIterativeEater wrapper = new IlluminaIterativeEater();
        wrapper.setFiles( new File[] { new File( rf.getFilename() ) } );  
        wrapper.setNormalizers( new QualityNormalizer[] { QualityNormalizer.SANGER } );
        wrapper.setReadType( READ_TYPE.SINGLE );

        Map<String, Set<String>> map = rns.findAllduplications( new DelegateIterator<IlluminaSpot, String>( wrapper.iterator() ) {
            @Override public String convert( IlluminaSpot obj )
            {
                return obj.read_name[ IlluminaSpot.FORWARD ];
            }
        }, 100 );
        
        return map.entrySet().stream().collect( Collectors.toMap( e -> String.format( "%s from %s", e.getKey(), stream_name ), 
                                                                  e -> e.getValue(), 
                                                                  ( e1, e2 ) -> e1, 
                                                                  LinkedHashMap::new ) );
    }

}
