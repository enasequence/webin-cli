package uk.ac.ebi.ena.rawreads;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.assembly.GenomeAssemblyWebinCliTest;
import uk.ac.ebi.ena.frankenstein.loader.common.feeder.DataFeederException;


public class 
FastqScannerTest 
{
    
    @Test public void 
    testSingle() throws SecurityException, NoSuchMethodException, DataFeederException, IOException, InterruptedException
    {
        URL  url1 = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/rawreads/EP0_GTTCCTT_S1.txt.gz" );
        FastqScanner fs = new FastqScanner();
        RawReadsFile rf = new RawReadsFile();
        
        rf.setFilename( new File( url1.getFile() ).getCanonicalPath() );
        
        ValidationResult vr = fs.checkFiles( rf );
        
        Assert.assertTrue( vr.getMessages( Severity.ERROR ).isEmpty() );
    }
    
    
    @Test public void 
    testSingleDuplications() throws SecurityException, NoSuchMethodException, DataFeederException, IOException, InterruptedException
    {
        URL  url1 = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/rawreads/EP0_GTTCCTT_S1.txt.dup.gz" );
        FastqScanner fs = new FastqScanner();
        RawReadsFile rf = new RawReadsFile();
        
        rf.setFilename( new File( url1.getFile() ).getCanonicalPath() );
        
        ValidationResult vr = fs.checkFiles( rf );
        
        Assert.assertEquals( vr.toString(), 2, vr.getMessages( Severity.ERROR ).size() );
    }


    @Test public void 
    testPaired() throws SecurityException, NoSuchMethodException, DataFeederException, IOException, InterruptedException
    {
        URL  url1 = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/rawreads/EP0_GTTCCTT_0.TXT.GZ" );
        FastqScanner fs = new FastqScanner();
        RawReadsFile rf = new RawReadsFile();
        
        rf.setFilename( new File( url1.getFile() ).getCanonicalPath() );
        
        ValidationResult vr = fs.checkFiles( rf );
        
        Assert.assertEquals( vr.toString(), 0, vr.getMessages( Severity.ERROR ).size() );
    }
    
    
    @Test public void 
    testPair() throws SecurityException, NoSuchMethodException, DataFeederException, IOException, InterruptedException
    {
        URL  url1 = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/rawreads/EP0_GTTCCTT_S1.txt.gz" );
        URL  url2 = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/rawreads/EP0_GTTCCTT_S2.txt.gz" );
        FastqScanner fs = new FastqScanner();
        RawReadsFile rf1 = new RawReadsFile();
        rf1.setFilename( new File( url1.getFile() ).getCanonicalPath() );
        
        RawReadsFile rf2 = new RawReadsFile();
        rf2.setFilename( new File( url2.getFile() ).getCanonicalPath() );

        ValidationResult vr = fs.checkFiles( rf1, rf1 );
        
        Assert.assertEquals( toString( vr ), 0, vr.getMessages( Severity.ERROR ).size() );
    }
    
    
    String
    toString( ValidationResult result ) throws IOException
    {
        StringWriter sw = new StringWriter();
        for( ValidationMessage<?> m : result.getMessages() )
        {
            sw.write( String.format( "%s: %s\n", m.getSeverity(), Arrays.asList( m.getParams() ) ) );
        }
        return sw.toString();
    }
    
    
    
    Path
    generateRandomFastq( int number_of_reads,
                         int min_name_len, 
                         int max_name_len, 
                         int read_len ) throws IOException
    {
        Path result = Files.createTempFile( "TEMP", ".fastq" );
        
        while( number_of_reads--> 0 )
        {
            StringBuilder read = new StringBuilder();
           
            read.append( "@" )
                .append( ThreadLocalRandom.current()
                                          .ints( ThreadLocalRandom.current().nextInt( min_name_len, max_name_len ), 48, 127 )
                                          .mapToObj( e -> String.valueOf( Character.toString( (char)e ) ) )
                                          .collect( Collectors.joining() ) )
                .append( '\n' )
                .append( ThreadLocalRandom.current()
                        .ints( read_len, 0, 5 )
                        .mapToObj( e -> e == 0 ? "A" : e == 1 ? "C" : e == 2 ? "G" : e == 3 ? "T" : "N" )
                        .collect( Collectors.joining() ) )
                .append( '\n' )
                .append( '+' )
                .append( '\n' )
                .append( ThreadLocalRandom.current()
                        .ints( read_len, 33, 33 + 64 )
                        .mapToObj( e -> String.valueOf( Character.toString( (char)e ) ) )
                        .collect( Collectors.joining() ) )
                .append( '\n' );
            Files.write( result, read.toString().getBytes(), StandardOpenOption.SYNC, StandardOpenOption.APPEND );
        }        
        return result;
            
    }


    @Test public void 
    testGeneratedSingleDuplications() throws SecurityException, NoSuchMethodException, DataFeederException, IOException, InterruptedException
    {
        FastqScanner fs = new FastqScanner();
        RawReadsFile rf = new RawReadsFile();
        Path path = generateRandomFastq( 1000, 2, 5, 80 );
        rf.setFilename( path.toString() );
        
        ValidationResult vr = fs.checkFiles( rf );
        
        Assert.assertEquals( toString( vr ), 2, vr.getMessages( Severity.ERROR ).size() );
    }

}
