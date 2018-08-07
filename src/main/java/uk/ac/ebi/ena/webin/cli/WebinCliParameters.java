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

package uk.ac.ebi.ena.webin.cli;

import java.io.File;

public class 
WebinCliParameters
{
    private File    manifestFile;
    private String  username;
    private String  password;
    private File    outputDir;
    private File    inputDir = new File( "." );
    private File    syslogFile = new File( "webin-cli.log" );
    private String  centerName;
    
    
    public File
    getManifestFile()
    {
        return manifestFile;
    }
    
    
    public String
    getUsername()
    {
        return username;
    }
    
    
    public String
    getPassword()
    {
        return password;
    }
    
    
    public File
    getOutputDir()
    {
        return outputDir;
    }
    
    
    public void
    setManifestFile( File manifestFile )
    {
        this.manifestFile = manifestFile;
    }
    
    
    public void
    setUsername( String username )
    {
        this.username = username;
    }
    
    
    public void
    setPassword( String password )
    {
        this.password = password;
    }
    
    
    public void
    setOutputDir( File outputDir )
    {
        this.outputDir = outputDir;
    }


    public File
    getInputDir()
    {
        return inputDir;
    }


    public void
    setInputDir( File inputDir )
    {
        this.inputDir = inputDir;
    }
    
    
    public void
    setCenterName( String centerName )
    {
        this.centerName = centerName;
    }

    
    public String
    getCenterName()
    {
        return centerName;
    }


    public File
    getSystemLogFile()
    {
        return syslogFile;
    }


    public void
    setSystemLogFile( File syslogFile )
    {
        this.syslogFile = syslogFile;
    }
}