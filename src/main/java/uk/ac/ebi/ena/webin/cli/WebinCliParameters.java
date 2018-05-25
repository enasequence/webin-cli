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