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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.List;

import uk.ac.ebi.ena.rawreads.VerboseLogger;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class 
ASCPService implements UploadService, VerboseLogger
{
    private static final String EXECUTABLE = "ascp";
    private String userName;
    private String password;

    static class 
    VerboseStreamConsumer extends Thread implements VerboseLogger
    {
        private Reader ireader;
        private long read_cnt;
        
        
        VerboseStreamConsumer( InputStream istream )
        {
            this.ireader = new InputStreamReader( istream, StandardCharsets.UTF_8 );
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
                    {
                        printfToConsole( "%c", (char)ch );
                        flushConsole();
                    }
                }
                
            } catch( IOException e )
            {
                ;
            }
        }
        
        
        public long
        getReadCnt()
        {
            return read_cnt;
        }
    }
    

    static class 
    StreamConsumer extends VerboseStreamConsumer
    {
        StreamConsumer( InputStream istream )
        {
            super( istream );
        }
        
        
        public void
        printfToConsole( String msg, Object... arg1 ) { }
    }

    
    @Override public boolean
    isAvaliable()
    {
        try
        {            
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec( EXECUTABLE + " -h", 
                                    new String[] { String.format( "PATH=%s", System.getenv( "PATH" ) ) } );
            
            new StreamConsumer( proc.getInputStream() ).start();
            new StreamConsumer( proc.getErrorStream() ).start();
            
            proc.waitFor();
            
            int exitVal = proc.exitValue();
            
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
    
       
    private String 
    getCommand( Path file_list, Path inputDir, String uploadDir )
    {
        return String.format( "%s --file-checksum=md5 -d --mode=send --overwrite=always --user=\"%s\" -QT -l300M -L- --src-base=%s " /* --source-prefix=\"%s\" */ + " --file-list=\"%s\" --host=webin.ebi.ac.uk \"%s\"",
                              EXECUTABLE,
                              this.userName, 
                              inputDir.normalize().toString(),
                              //inputDir.normalize().toString(),
                              file_list,
                              uploadDir );
    }
    
    
    @Override public void
    ftpDirectory( List<File> uploadFilesList,
                  String uploadDir,
                  Path inputDir )
    {
        try
        {            
            String file_list = createUploadList( uploadFilesList, inputDir );
            String command = getCommand( Files.write( Files.createTempFile( "FILE", "LIST", new FileAttribute<?>[] {} ), 
                                                      file_list.getBytes(), 
                                                      StandardOpenOption.CREATE, StandardOpenOption.SYNC ),
                                         inputDir.toAbsolutePath(),
                                         uploadDir );
            
            printfToConsole( "Invoking: %s\n", command );
            flushConsole();

            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec( command, 
                                    new String[] { String.format( "ASPERA_SCP_PASS=%s", this.password ), 
                                                   String.format( "PATH=%s", System.getenv( "PATH" ) ) } );
            
            new VerboseStreamConsumer( proc.getInputStream() ).start();
            new VerboseStreamConsumer( proc.getErrorStream() ).start();
            
            proc.waitFor();
            
            int exitVal = proc.exitValue();
            
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
