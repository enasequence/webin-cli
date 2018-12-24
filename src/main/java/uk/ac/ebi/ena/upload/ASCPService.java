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

package uk.ac.ebi.ena.upload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import uk.ac.ebi.ena.rawreads.VerboseLogger;
import uk.ac.ebi.ena.webin.cli.WebinCliException;


public class 
ASCPService implements UploadService, VerboseLogger
{
    private static final String EXECUTABLE = "ascp";
    private String userName;
    private String password;

    
    class 
    VerboseStreamConsumer extends Thread
    {
        private Reader ireader;
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
                ;
            }
        }
        
        
        protected void 
        printFlush( char ch )
        {
            ASCPService.this.printfToConsole( "%c", (char)ch );
            ASCPService.this.flushConsole();
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

    
    @Override public boolean
    isAvaliable()
    {
        try
        {            
            ExecutorService es = Executors.newFixedThreadPool( 2 );
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec( EXECUTABLE + " -h", 
                                    new String[] { String.format( "PATH=%s", System.getenv( "PATH" ) ) } );
            
            es.submit( new StreamConsumer( proc.getInputStream() ) );
            es.submit( new StreamConsumer( proc.getErrorStream() ) );
            
            proc.waitFor();
            int exitVal = proc.exitValue();
            
            es.shutdown();
            es.awaitTermination( 20, TimeUnit.SECONDS );
            
            if( 0 != exitVal )
                return false;
            
        } catch( Throwable t )
        {
            return false;
        }
        
        return true;
    }
    
    
    @Override public void
    connect( String userName, String password )
    {
        this.password = password;
        this.userName = userName;
        
    }

    
    private String
    createUploadList( List<File> uploadFilesList, Path inputDir )
    {
        StringBuilder sb = new StringBuilder();
        for( File f : uploadFilesList )
        {
            String from = f.isAbsolute() ? f.toString() : inputDir.resolve( f.toPath() ).normalize().toString();
            sb.append( String.format( "%s\n", from ) );
        }
        return sb.toString();
    }
    
       
    private String[] 
    getCommand( Path file_list, Path inputDir, String uploadDir )
    {
        return new String[] { EXECUTABLE, 
                              "--file-checksum=md5",
                              "-d",
                              "--mode=send",
                              "--overwrite=always",
                              "-QT",
                              "-l300M",
                              //"-L-",
                              "--host=webin.ebi.ac.uk",
                              String.format( "--user=\"%s\"", this.userName ),
                              String.format( "--src-base=\"%s\"", inputDir.normalize().toString().replaceAll( " ", "\\\\ " ) ),
                              String.format( "--file-list=\"%s\"", file_list ),
                              String.format( "\"%s\"", uploadDir ) };
    }
    
    
    @Override public void
    ftpDirectory( List<File> uploadFilesList,
                  String uploadDir,
                  Path inputDir )
    {
        try
        {      
            String file_list = createUploadList( uploadFilesList, inputDir );
            String[] command = getCommand( Files.write( Files.createTempFile( "FILE", "LIST", new FileAttribute<?>[] {} ), 
                                                      file_list.getBytes(), 
                                                      StandardOpenOption.CREATE, StandardOpenOption.SYNC ),
                                         inputDir.toAbsolutePath(),
                                         uploadDir );
            
            printfToConsole( "Invoking: %s\n", Arrays.stream( command ).collect( Collectors.joining( " " ) ) );
            flushConsole();

            ProcessBuilder pb = System.getProperty( "os.name" ).toLowerCase().contains( "win" ) ? new ProcessBuilder( "cmd", "/c", Arrays.stream( command ).collect( Collectors.joining( " " ) ) ) 
                                                                                                : new ProcessBuilder( "sh", "-c", Arrays.stream( command ).collect( Collectors.joining( " " ) ) );
            pb.environment().put( "ASPERA_SCP_PASS", this.password );
            pb.environment().put( "PATH", System.getenv( "PATH" ) );
            pb.directory( null );
            pb.redirectOutput( Redirect.INHERIT );
            
            ExecutorService es = Executors.newFixedThreadPool( 2 );
            
            Process proc = pb.start();
            es.submit( new VerboseStreamConsumer( proc.getInputStream() ) );
            es.submit( new StreamConsumer( proc.getErrorStream() ) );
            
            proc.waitFor();
            
            int exitVal = proc.exitValue();
            
            es.shutdown();
            es.awaitTermination( 30, TimeUnit.SECONDS );
            
            if( 0 != exitVal )
                throw WebinCliException.createSystemError( "Unable to upload files using ASPERA" );
            
        } catch( Throwable t )
        {
            throw WebinCliException.createSystemError( "Unable to upload files using ASPERA " + t.getMessage() );
        }
    }

    
    @Override public void
    disconnect()
    {
        //do nothing
        this.password = null;
        this.userName = null;
    }
}
