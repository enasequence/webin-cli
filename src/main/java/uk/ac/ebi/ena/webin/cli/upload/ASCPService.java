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

package uk.ac.ebi.ena.webin.cli.upload;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.ebi.ena.webin.cli.utils.ShellExec;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;


public class 
ASCPService implements UploadService
{
    private static final String EXECUTABLE = "ascp";
    private String userName;
    private String password;

    @Override public boolean
    isAvaliable()
    {
        try
        {            
            int exitVal = new ShellExec( EXECUTABLE + " -h", new HashMap<String, String>()  { private static final long serialVersionUID = 1L; { put( "PATH", System.getenv( "PATH" ) ); } } ).exec();
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
            String[] command = getCommand( Files.write( Files.createTempFile( "FILE", "LIST"),
                                                      file_list.getBytes(), 
                                                      StandardOpenOption.CREATE, StandardOpenOption.SYNC ),
                                         inputDir.toAbsolutePath(),
                                         uploadDir );
            
            String cmd = String.join(" ", command);
            Map<String, String> vars = new HashMap<>();
            vars.put( "ASPERA_SCP_PASS", this.password );
            vars.put( "PATH", System.getenv( "PATH" ) );
            int exitVal = new ShellExec( cmd, vars ).exec( true );
            if( 0 != exitVal )
                throw WebinCliException.systemError(WebinCliMessage.Aspera.UPLOAD_ERROR.format());
            
        } catch( Exception e )
        {
            throw WebinCliException.systemError(WebinCliMessage.Aspera.UPLOAD_ERROR.format(), e.getMessage());
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
