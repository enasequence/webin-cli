package uk.ac.ebi.ena.rawreads;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
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
import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class 
FastqScanner 
{
    private static final int BLOOM_EXPECTED_READS = 800_000_000;
    protected static final int MAX_LABEL_SET_SIZE = 10;
    
    private final int expected_size;
    
    
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
          ReadNameSet<SimpleEntry<String, Long>> rns,
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
//                    String name = slash_idx == -1 ? spot.bname 
//                                                  : spot.bname.substring( 0, slash_idx );
                    String label = slash_idx == -1 ? stream_name
                                                   : spot.bname.substring( slash_idx + 1 );
                    
                    if( labels.size() < MAX_LABEL_SET_SIZE )
                        labels.add( label );
                    
                    count.incrementAndGet();
                    rns.add( spot.bname, new SimpleEntry<String, Long>( stream_name, read_no.getAndIncrement() ) );
                }  
            } );
            
            df.start();
            df.join();
            return df.isOk() ? df.getFieldFeedCount() > 0 ? null : new DataFeederException( 0, "Empty file" ) : (DataFeederException)df.getStoredException().getCause();
        }
    }

    
    private ValidationResult
    checkSingleFile( RawReadsFile rf, 
                     String stream_name, 
                     Set<String> labelset, 
                     ReadNameSet<SimpleEntry<String, Long>> rns ) throws SecurityException, NoSuchMethodException, DataFeederException, IOException, InterruptedException
    {
        ValidationResult vr = new ValidationResult();
        AtomicLong count = new AtomicLong();
        DataFeederException t = read( rf, stream_name, labelset, rns, count );
                        
        if( null != t )
        {
            ValidationMessage<Origin> vm = new ValidationMessage<>( Severity.ERROR, ValidationMessage.NO_KEY, t.getMessage() );
            vm.append( new DefaultOrigin( String.format( "%s:%d", stream_name, t.getLineNo() ) ) );
            
            vm.setThrowable( t );
            vr.append( vm );
        } else
        {
            vr.append( new ValidationMessage<>( Severity.INFO, ValidationMessage.NO_KEY, String.format( "%s: Collected %d reads", stream_name, count.get() ) ) );
            vr.append( new ValidationMessage<>( labelset.size() > 2 ? Severity.ERROR : Severity.INFO, 
                                                ValidationMessage.NO_KEY, 
                                                String.format( "%s: Collected %d read labels: %s", stream_name, labelset.size(), labelset ) ) );
            vr.append( new ValidationMessage<>( Severity.INFO, ValidationMessage.NO_KEY, String.format( "%s: Has possible dublicates: " + rns.hasPossibleDuplicates(), stream_name ) ) );
        }
        return vr;
    }

    
    private String
    formatStreamName( int index, String filename )
    {
        return String.format( "File[%d]: %s", index++, filename );
    }
    
    
    public ValidationResult
    checkFiles( RawReadsFile...rfs ) throws SecurityException, NoSuchMethodException, DataFeederException, IOException, InterruptedException
    {
        Set<String> labelset = new HashSet<>();
        ReadNameSet<SimpleEntry<String, Long>> rns = new ReadNameSet<>( expected_size );
        ValidationResult vr = new ValidationResult();
        int index = 1;
        for( RawReadsFile rf : rfs )
            vr.append( checkSingleFile( rf, formatStreamName( index++, rf.getFilename() ), labelset, rns ) );

        
        //extra check for suspected reads
        if( rns.hasPossibleDuplicates() )
        {
            index = 1;
            for( RawReadsFile rf : rfs )
            {
                String stream_name = formatStreamName( index++, rf.getFilename() );
                ValidationResult v = checkSuspected( rf, stream_name, rns );
                if( !v.isValid() )
                    vr.append( v );
            }
            
            if( vr.isValid() )
                vr.append( new ValidationMessage<>( Severity.INFO, ValidationMessage.NO_KEY, "No duplicates confirmed." ) );
        }

        return vr;
    }

    
    private ValidationResult
    checkSuspected( RawReadsFile rf,
                    String stream_name,
                    ReadNameSet<SimpleEntry<String, Long>> rns ) throws InterruptedException, SecurityException, NoSuchMethodException, DataFeederException, IOException
    {
        ValidationResult vr = new ValidationResult();
        try( InputStream is = openFileInputStream( Paths.get( rf.getFilename() ) ); )
        {
            //String stream_name = rf.getFilename();
            
            AbstractDataFeeder<DataSpot> df = new AbstractDataFeeder<DataSpot>( is, DataSpot.class ) 
            {
                DataSpotParams params = DataSpot.defaultParams();
                
                @Override protected DataSpot 
                newFeedable()
                {
                    return new DataSpot( QualityNormalizer.NONE, "", params );
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
//                    String name = slash_idx == -1 ? spot.bname 
//                                                  : spot.bname.substring( 0, slash_idx );
                    String label = slash_idx == -1 ? stream_name
                                                   : spot.bname.substring( slash_idx + 1 );
                    
                    Set<SimpleEntry<String, Long>> set = rns.getDuplicateLocations( spot.bname, new SimpleEntry<>( stream_name, read_no.getAndIncrement() ) );
                    if( !set.isEmpty() )
                    {
                        for( SimpleEntry<String, Long> e : set )
                        {
                         //   if( e.getKey().equals( label ) )
                            {
                                vr.append( new ValidationMessage<>( Severity.ERROR, 
                                                                    ValidationMessage.NO_KEY, 
                                                                    String.format( "%s Read %d: %s has dublicate(s) %s", 
                                                                                   stream_name,
                                                                                   read_no.get(), 
                                                                                   spot.bname, 
                                                                                   set.toString() ) ) );
                            }
                        }
                    }
                }  
            } );
        
            df.start();
            df.join();
            if( !df.isOk() )
                throw WebinCliException.createSystemError( "Unable to re-read file" );
        
            return vr;
        }
    }
}
