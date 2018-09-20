/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.rawreads;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

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
import uk.ac.ebi.ena.webin.cli.WebinCliReporter;

public class 
FastqScanner 
{
    private static final String S_READ_D = "%s, Read %d";
    protected static final int MAX_LABEL_SET_SIZE = 10;
    private static final int PAIRING_THRESHOLD = 30;
    
    private final int expected_size;
    private Set<String> labelset = new HashSet<>();
    private AtomicBoolean paired = new AtomicBoolean();
    private boolean verbose = true;
    
    
    public
    FastqScanner( int expected_size )
    {
        this.expected_size = expected_size;
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
          BloomWrapper pairing,
          BloomWrapper duplications,
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
                    pairing.add( name );
                    duplications.add( spot.bname );
                    
                    if( 0 == count.get() % 1000 )
                        printProcessedReadNumber( count );
                }  
            } );

            printlnToConsole( "Processing file " + rf.getFilename() );
            flushConsole();
            df.start();
            df.join();
            DataFeederException result = df.isOk() ? df.getFieldFeedCount() > 0 ? null : new DataFeederException( 0, "Empty file" ) : (DataFeederException)df.getStoredException().getCause();
            printProcessedReadNumber( count );
            printlnToConsole();
            return result;
        }
    }


    private void 
    printProcessedReadNumber( AtomicLong count )
    {
        printfToConsole( "\rProcessed %16d read(s)", count.get() );
        flushConsole();
    }

    
    private ValidationResult
    checkSingleFile( RawReadsFile rf, 
                     String stream_name, 
                     Set<String> labelset,
                     BloomWrapper pairing,
                     BloomWrapper duplications ) throws SecurityException, NoSuchMethodException, DataFeederException, IOException, InterruptedException
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
            vr.append( fMsg( Severity.ERROR, "Unusual amount of files. Can accept only 1 or 2, but got " + ( null == rfs ? "null" : rfs.length ) ) );
            return vr;
        }   
        
        BloomWrapper duplications = new BloomWrapper( expected_size );
        BloomWrapper pairing = new BloomWrapper( expected_size / 10 );

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
            double pairing_level = (double)pairing.getPossibleDuplicateCount() / ( (double)( pairing.getAddsNumber() - pairing.getPossibleDuplicateCount() ) );
            pairing_level = 100 * ( 1 < pairing_level ? 1/ pairing_level : pairing_level );
            String msg = String.format( "Pairing percentage: %.2f%%", pairing_level );
            vr.append( fMsg( Severity.INFO, msg ) ); 
            printlnToConsole( msg );
            
            //TODO: estimate bloom false positives impact
            if( (double)PAIRING_THRESHOLD > pairing_level )
            {
                //terminal error
                msg = String.format( "Detected paired fastq submission with less than %d%% of paired reads", PAIRING_THRESHOLD );
                vr.append( fMsg( Severity.ERROR, msg ) );
                printlnToConsole( msg );
            }

        } else if( labelset.size() > 2 )
        {
            String msg = String.format( "When submitting paired reads using two Fastq files the reads must follow Illumina paired read naming conventions. "
                                      + "This was not the case for the submitted Fastq files: %s. Unable to determine pairing from set: %s",
                                        rfs,
                                        labelset.stream().limit( 10 ).collect( Collectors.joining( ",", "", 10 < labelset.size() ? "..." : "" ) ) ); 
            vr.append( fMsg( Severity.ERROR, msg ) ); 
            printlnToConsole( msg );
        }   
        
        //extra check for suspected reads
        if( duplications.hasPossibleDuplicates() )
        {
    
            Map<String, Set<String>> result = new LinkedHashMap<>();
            ValidationResult dvr = new ValidationResult();
            result = findAllduplications( duplications, 100, rfs );
                
            dvr.append( result.entrySet().stream().map( ( e ) -> fMsg( Severity.ERROR, 
                                                                       String.format( "Multiple (%d) occurance of read name \"%s\" at: %s\n",
                                                                                      e.getValue().size(), 
                                                                                      e.getKey(), 
                                                                                      e.getValue().toString() ) ) ).collect( Collectors.toList() ) );
            if( dvr.isValid() )
                dvr.append( fMsg( Severity.INFO, "No duplicates confirmed." ) );
            
            vr.append( dvr );
        }

        return vr;
    }

    
    private void
    flushConsole()
    {
        if( verbose )
            WebinCliReporter.flushConsole();
    }
    

    private void
    printlnToConsole( String msg )
    {
        if( verbose )
            WebinCliReporter.printlnToConsole( msg );
    }

    
    private void
    printlnToConsole()
    {
        if( verbose )
            WebinCliReporter.printlnToConsole();
    }

    
    private void
    printfToConsole( String msg, Object... arg1 )
    {
        if( verbose )
            WebinCliReporter.printfToConsole( msg, arg1 );
    }

    
    public Map<String, Set<String>>
    findAllduplications( BloomWrapper duplications, int limit, RawReadsFile...rfs )
    {
        Map<String, Integer> counts = new HashMap<>( limit );
        Map<String, Set<String>> results = new LinkedHashMap<>( limit );
        for( RawReadsFile rf: rfs )
        {
            printlnToConsole( "Performing additional checks for file " + rf.getFilename() );

            long index = 1;
            Map<String, Long> first_seen = new LinkedHashMap<>( limit );
            
            IlluminaIterativeEater wrapper = new IlluminaIterativeEater();
            wrapper.setFiles( new File[] { new File( rf.getFilename() ) } );  
            wrapper.setNormalizers( new QualityNormalizer[] { QualityNormalizer.SANGER } );
            wrapper.setReadType( READ_TYPE.SINGLE );

            Iterator<String> read_name_iterator = new DelegateIterator<IlluminaSpot, String>( wrapper.iterator() ) {
                @Override public String convert( IlluminaSpot obj )
                {
                    return obj.read_name[ IlluminaSpot.FORWARD ];
                }
            };
            
            while( read_name_iterator.hasNext() )
            {
                String read_name = read_name_iterator.next();
                if( duplications.getSuspected().contains( read_name ) )
                {
                    counts.put( read_name, counts.getOrDefault( read_name, 0 ) + 1 );
                    Set<String> dlist = results.getOrDefault( read_name, new LinkedHashSet<>() );
                    first_seen.putIfAbsent( read_name, index );
                    dlist.add( String.format( "%s, read %s", rf.getFilename(), index ) );
                    results.put( read_name, dlist );
                }
                index ++;
            }
        }
        
        return results.entrySet()
                      .stream()
                      .filter( e-> counts.get( e.getKey() ).intValue() > 1 )
                      .limit( limit )
                      .collect( Collectors.toMap( e -> e.getKey(), e -> e.getValue(), ( v1, v2 ) -> v1, LinkedHashMap::new ) );
    }
}