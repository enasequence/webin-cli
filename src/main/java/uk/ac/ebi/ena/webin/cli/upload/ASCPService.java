/*
 * Copyright 2018-2023 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.upload;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.utils.RetryUtils;
import uk.ac.ebi.ena.webin.cli.utils.ShellExec;


public class ASCPService implements UploadService
{
    private final static String SERVER = "webin.ebi.ac.uk";

    private static final Logger log = LoggerFactory.getLogger(ASCPService.class);

    private static final String EXECUTABLE = "ascp";
    private String userName;
    private String password;

    @Override public boolean
    isAvailable()
    {
        try
        {
            HashMap<String, String> vars = new HashMap<>();
            vars.put( "PATH", System.getenv( "PATH" ) );

            int exitVal = new ShellExec( EXECUTABLE + " -h", vars ).exec();
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
    getCommand( Path file_list, String uploadDir )
    {
        return new String[] { EXECUTABLE, 
                              "--file-checksum=md5",
                              "-d",
                              "--mode=send",
                              "--overwrite=always",
                              "-QT",
                              "-l300M",
                              //"-L-",
                              String.format("--host=%s", SERVER),
                              String.format( "--user=\"%s\"", this.userName ),
                              String.format( "--file-list=\"%s\"", file_list ),
                              String.format( "\"%s\"", uploadDir ) };
    }
    
    
    @Override public void
    upload(List<File> uploadFilesList,
           String uploadDir,
           Path inputDir )
    {
        log.info("Uploading files to : {}", SERVER);

        try
        {      
            String file_list = createUploadList( uploadFilesList, inputDir );
            String[] command = getCommand( Files.write( Files.createTempFile( "FILE", "LIST"),
                                                      file_list.getBytes(), 
                                                      StandardOpenOption.CREATE, StandardOpenOption.SYNC ),
                                         uploadDir );
            
            String cmd = String.join(" ", command);
            Map<String, String> vars = new HashMap<>();
            vars.put( "ASPERA_SCP_PASS", this.password );
            vars.put( "PATH", System.getenv( "PATH" ) );

            RetryUtils.executeWithRetry(context -> {
                int exitVal = new ShellExec( cmd, vars ).exec( true );

                // Even when the process completes without exception, throw error as long as exit code is not 0 so a
                // retry can be attempted.
                if( 0 != exitVal )
                    throw WebinCliException.systemError(WebinCliMessage.ASCP_UPLOAD_ERROR.text());

                return null;
            }, context -> log.warn("Retrying file upload."), Exception.class);
        } catch (WebinCliException ex) {
            throw ex;
        } catch( Exception ex ) {
            throw WebinCliException.systemError(WebinCliMessage.ASCP_UPLOAD_ERROR.text(), ex.getMessage());
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
