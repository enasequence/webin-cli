/*******************************************************************************
 * Copyright 2013 EMBL-EBI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package uk.ac.ebi.ena.rawreads.refs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.HttpsURLConnection;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamFiles;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.cram.ref.ReferenceSource;
import htsjdk.samtools.util.Log;
import htsjdk.samtools.util.Log.LogLevel;
import net.sf.cram.ref.PathPattern;
import uk.ac.ebi.ena.rawreads.VerboseLogger;

public class 
CramReferenceInfo implements VerboseLogger
{
    private static final String REF_INFO_PATH = "/.webin-cli/cram-ref-info/%2s/%2s/%s";
    private static final String JAVA_IO_TMPDIR_PROPERTY_NAME = "java.io.tmpdir";
    private static final String USER_HOME_PROPERTY_NAME = "user.home";    
    
    private static final String service_link = "https://www.ebi.ac.uk/ena/cram/sequence/%32s/metadata";
    private static final String info_path = null != System.getProperty( USER_HOME_PROPERTY_NAME ) ? System.getProperty( USER_HOME_PROPERTY_NAME )      + REF_INFO_PATH 
                                                                                                  : System.getProperty( JAVA_IO_TMPDIR_PROPERTY_NAME ) + REF_INFO_PATH ;
    private ScriptEngine engine;
    private PathPattern  cache_pattern;
    
    
    public 
    CramReferenceInfo()
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        this.engine = sem.getEngineByName( "javascript" );
        this.cache_pattern = new PathPattern( info_path );
    }
    
    
    private String
    fetchData( URL url ) throws IOException
    {
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        StringBuilder sb = new StringBuilder();
        try( BufferedReader br = new BufferedReader( new InputStreamReader( con.getInputStream() ) ) )
        {
            String input;
            while( ( input = br.readLine() ) != null )
                sb.append( input ).append( '\n' );
        }
        return sb.toString();
    }
    
    
    public static class
    ReferenceInfo
    {
        private String id, md5; 
        private long length;
        private Object trunc512;
        private List<Map<String, Object>> aliases;
        
        
        public String getId() { return id; }
        public String getMd5() { return md5; }
        public long getLength() { return length; }
        public Object getTrunc512() { return trunc512; }
        public List<Map<String, Object>> getAliases() { return aliases; }
    }

    
    private ReferenceInfo 
    parseNCBIJson( String json ) throws IOException, ScriptException, URISyntaxException 
    {
        String script = "Java.asJSONCompatible(" + json + ")";
        Object result = this.engine.eval( script );
        
        @SuppressWarnings( "unchecked" )
        Map<String, Object> content = (Map<String, Object>) result;
        
        @SuppressWarnings( "unchecked" )
        Map<String, Object> metadata = (Map<String, Object>)content.get( "metadata" );
        
        ReferenceInfo info = new ReferenceInfo();
        info.id  = (String)metadata.get( "id" );
        info.md5 = (String)metadata.get( "md5" );
        info.trunc512       = (String)metadata.get( "trunc512" );
        try
        {
            info.length   =  ( (Number) metadata.get( "length" ) ).longValue();
        } catch( NullPointerException npe )
        {
            ;
        }
        
        @SuppressWarnings( "unchecked" )
        List<Map<String, Object>> aliases = (List<Map<String, Object>>) metadata.get( "aliases" );
        
        info.aliases       = aliases;
        return info;
    }


    public ReferenceInfo
    fetchReferenceMetadata( String md5, PrintStream... pss )
    {
        for( int i = 0; i < 4; ++i )
        try
        {
            String data = fetchData( new URL( String.format( service_link, md5 ) ) );
            ReferenceInfo info = parseNCBIJson( data );
            
            if( null != pss )
                Arrays.stream( pss ).forEachOrdered( ps -> ps.print( data ) );
            
            return info;
            
        } catch( Throwable t )
        {
            try
            {
                Thread.sleep( 1000 );
            } catch( InterruptedException ie )
            {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return new ReferenceInfo();
    }
    
    
    private boolean
    findOnDisk( String md5 )
    {
        String path = cache_pattern.format( md5 );
        Path entry = Paths.get( path );
        if( Files.exists( entry, LinkOption.NOFOLLOW_LINKS ) )
        {
            if( 1 != entry.toFile().length() )
                return false; //throw new RuntimeException( "The reference sequence is too long: " + md5 );
            
            return true;
        }
        
        return false;
    }
    
    
    private boolean
    putOnDisk( String md5 )
    {
        try
        {
            String path = cache_pattern.format( md5 );
            Path entry = Paths.get( path );
            Files.createDirectories( entry.getParent() );
            Files.write( entry, new byte[] { '1' }, StandardOpenOption.CREATE, StandardOpenOption.SYNC );
            return true;
        } catch( IOException ioe )
        {
            return true;
        }
    }
    
    
    
    public Map<String, Boolean>
    confirmFileReferences( File file ) throws IOException
    {
        ThreadPoolExecutor es = new ThreadPoolExecutor( 10, 10, 1, TimeUnit.HOURS, new ArrayBlockingQueue<Runnable>( 10 ) );
        Map<String, Boolean> result = new ConcurrentHashMap<>(); 
        Log.setGlobalLogLevel( LogLevel.ERROR );
        SamReaderFactory.setDefaultValidationStringency( ValidationStringency.SILENT );
        SamReaderFactory factory = SamReaderFactory.make();
        factory.referenceSource( new ReferenceSource( (File) null ) );
        SamInputResource ir = SamInputResource.of( file );
        File indexMaybe = SamFiles.findIndex( file );
        //System.out.println( "proposed index: " + indexMaybe );
        AtomicLong count = new AtomicLong();
        try( SamReader reader = factory.open( ir ); )
        {
            printfToConsole( "Checking reference existance in the CRAM reference registry for %s\n", file.getPath() );
            es.prestartAllCoreThreads();
            for( SAMSequenceRecord sequenceRecord : reader.getFileHeader().getSequenceDictionary().getSequences() )
            {
                es.getQueue().put( new Runnable()
                {
                    public void 
                    run()
                    {
                        count.incrementAndGet();
                        String md5 = sequenceRecord.getAttribute( SAMSequenceRecord.MD5_TAG );
                        result.computeIfAbsent( md5, k -> {
                            if( findOnDisk( md5 ) )
                            {
                                return true;
                            } else
                            {
                                ReferenceInfo info = fetchReferenceMetadata( md5 );
                                return k.equals( info.getMd5() ) ? putOnDisk( md5 ) : false; 
                            }
                        } );
                    
                        if( 0 == count.get() % 10 )
                            printProcessedReferenceNumber( count );
                    }
                } );
            }
            
            es.shutdown();
            es.awaitTermination( 1, TimeUnit.HOURS );
            
            printProcessedReferenceNumber( count );
            printfToConsole( "\n" );

        } catch( InterruptedException e )
        {
            Thread.currentThread().interrupt();
        } 
        return result;
    }
    
    
    private void 
    printProcessedReferenceNumber( AtomicLong count )
    {
        printfToConsole( "\rChecked %16d references(s)", count.get() );
        flushConsole();
    }

    
    public static void
    main( String args[] ) throws MalformedURLException, IOException, ScriptException, URISyntaxException, InterruptedException
    {
        CramReferenceInfo m = new CramReferenceInfo();
        m.fetchReferenceMetadata( args[ 0 ] /* "a0d9851da00400dec1098a9255ac712e"*/, System.out );
    }
        
}
