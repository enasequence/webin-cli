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

package uk.ac.ebi.ena.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class 
ShellExec
{
    private static final Logger log = LoggerFactory.getLogger(ShellExec.class);

    class 
    VerboseStreamConsumer extends Thread
    {
        private final Reader ireader;
        private long read_cnt;
        
        
        VerboseStreamConsumer( InputStream istream )
        {
            this.ireader = new InputStreamReader( istream, StandardCharsets.UTF_8 );
            setName( getClass().getSimpleName() );
        }
        
        
        @Override public void
        run()
        {
            try
            {
                int ch = 0;
                while( -1 != ( ch = ireader.read() ) )
                {
                    ++read_cnt;
                    if( ch > 0 )
                        printFlush( (char)ch );
                }
                
            } catch( IOException e )
            {
            }
        }
        
        
        protected void 
        printFlush( char ch )
        {
            log.info( String.format( "%c", (char)ch ) );
        }
        
        
        public long
        getReadCnt()
        {
            return read_cnt;
        }
    }
    

    class 
    StreamConsumer extends VerboseStreamConsumer
    {
        StreamConsumer( InputStream istream )
        {
            super( istream );
        }
        
        
        @Override protected void
        printFlush( char ch ) {}
    }


    public 
    ShellExec( String command, Map<String, String> vars )
    {
        this.command = command;
        this.vars = vars;
    }
    
    
    private final String command;
    private final Map<String, String> vars;

    
    public int
    exec() throws IOException, InterruptedException
    {
        return exec( false );
    }
    
    
    public int
    exec( boolean verbose ) throws IOException, InterruptedException
    {
        return exec( getCommand(), getVars(), verbose );
    }

    
    public Map<String, String> 
    getVars()
    {
        return vars;
    }


    public String 
    getCommand()
    {
        return command;
    }

    
    private int
    exec(String command, Map<String, String> vars, boolean verbose) throws IOException, InterruptedException
    {
        ExecutorService es = null;
        try
        {   
            es = Executors.newFixedThreadPool( 2 );
            ((ThreadPoolExecutor) es).prestartAllCoreThreads();

            if( verbose )
            {
                log.info( String.format( "Invoking: %s\n", command ) );
            }
            
            ProcessBuilder pb = System.getProperty( "os.name" ).toLowerCase().contains( "win" ) ? new ProcessBuilder( "cmd", "/c", command )
                                                                                                : new ProcessBuilder( "sh", "-c",  command );
            pb.environment().putAll( vars );
            pb.directory( null );
            if( verbose )
                pb.redirectOutput( Redirect.INHERIT );
            
            Process proc = pb.start();
            
            es.submit( verbose ? new VerboseStreamConsumer( proc.getInputStream() ) : new StreamConsumer( proc.getInputStream() ) );
            es.submit( new StreamConsumer( proc.getErrorStream() ) );
            return proc.waitFor();
    
        } finally
        {
            es.shutdown();
            es.awaitTermination( 30, TimeUnit.SECONDS );
        }
    }
}
