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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class 
CramReferenceInfo 
{
    private static String service_link = "https://www.ebi.ac.uk/ena/cram/sequence/%32s/metadata";
    private ScriptEngine engine;
    
    
    public 
    CramReferenceInfo()
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        this.engine = sem.getEngineByName( "javascript" );
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
        for( int i = 0; i < 2; ++i )
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
    
    
    public Map<String, Boolean>
    confirmFileReferences( File file ) throws IOException
    {
        Map<String, Boolean> result = new HashMap<>(); 
        Log.setGlobalLogLevel( LogLevel.ERROR );
        SamReaderFactory.setDefaultValidationStringency( ValidationStringency.SILENT );
        SamReaderFactory factory = SamReaderFactory.make();
        factory.referenceSource( new ReferenceSource( (File) null ) );
        SamInputResource ir = SamInputResource.of( file );
        File indexMaybe = SamFiles.findIndex( file );
        //System.out.println( "proposed index: " + indexMaybe );
        try( SamReader reader = factory.open( ir ); )
        {
            for( SAMSequenceRecord sequenceRecord : reader.getFileHeader().getSequenceDictionary().getSequences() )
            {
                String md5 = sequenceRecord.getAttribute( SAMSequenceRecord.MD5_TAG );
                result.computeIfAbsent( md5, k -> { 
                    ReferenceInfo info = fetchReferenceMetadata( md5 );
                    return k.equals( info.getMd5() );
                } );
            }
        } 
        return result;
    }
    
    
    public static void
    main( String args[] ) throws MalformedURLException, IOException, ScriptException, URISyntaxException, InterruptedException
    {
        CramReferenceInfo m = new CramReferenceInfo();
        m.fetchReferenceMetadata( /* args[ 0 ] */ "a0d9851da00400dec1098a9255ac712e", System.out );
    }
        
}
