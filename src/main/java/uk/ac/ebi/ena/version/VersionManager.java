package uk.ac.ebi.ena.version;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import javax.script.ScriptException;

import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.ena.version.LatestRelease.GitHubReleaseAsset;
import uk.ac.ebi.ena.version.LatestRelease.GitHubReleaseInfo;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliReporter;

public class 
VersionManager 
{
    static
    {
        System.setProperty( "log4j.ignoreTCL", Boolean.toString( true ) );
    }
    
    
    GitHubReleaseInfo info;
    
    VersionManager(  )
    {
    }
    
    
    long
    download( URL url, Path path ) throws IOException
    {
        long downloaded = 0;
        try( InputStream is = new BufferedInputStream( url.openStream() );
             OutputStream os = new BufferedOutputStream( Files.newOutputStream( path,
                                                                                StandardOpenOption.CREATE, 
                                                                                StandardOpenOption.TRUNCATE_EXISTING, 
                                                                                StandardOpenOption.SYNC  ) ) )
        {
            int ch = 0;
            while( -1 != ( ch = is.read() ) )
            {
                downloaded++;
                os.write( ch );
            }
        }
        return downloaded;
    }
    
    
    private File
    getClassPathFolder()
    {
        Set<File> cp_list = Arrays.stream( System.getProperty( "java.class.path" )
                                  .split( ":" ) )
                                  .map( e -> new File( e ).getParentFile() )
                                  .collect( Collectors.toSet() );
        return cp_list.iterator().next();
    }

    
    private boolean
    findInClassPath( File file )
    {
        List<File> cp_list = Arrays.stream( System.getProperty( "java.class.path" ).split( ":" ) ).map( e -> new File( e ) ).collect( Collectors.toList() );        
        for( File f : cp_list ) 
        {
            if( f.getName().equals( file.getName() ) )
                return true;
        }        
        return false;
    }
    
    
    private Object
    invokeMain( ClassLoader cl, String args[] ) throws IOException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
    {
        Manifest m = new Manifest( cl.getResourceAsStream( "META-INF/MANIFEST.MF" ) );
        String main_class_name = m.getMainAttributes().getValue( Name.MAIN_CLASS );
        
        Class<?> klass = cl.loadClass( main_class_name );
        Method main = klass.getMethod( "main", java.lang.String[].class );
        return main.invoke( klass, new Object[] { args } );
    }
    
    
    public int
    launchLatest( String[] args ) throws IOException, InterruptedException, ScriptException, URISyntaxException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        LatestRelease lr = new LatestRelease();
        info = lr.getLatestInfo();
        if( 1 != info.assets.size() )
            throw WebinCliException.createSystemError( "Unable to fetch information about latest release" );
        
        GitHubReleaseAsset ra = info.assets.get( 0 );
        
        if( !"uploaded".equals( ra.state ) )
            throw WebinCliException.createSystemError( "Unable to fetch information about latest release" );
        
        File file = new File( getClassPathFolder(), ra.name );
        
        if( findInClassPath( file ) )
            return (int) invokeMain( this.getClass().getClassLoader(), args );
        
        if( !file.exists() || ra.size != file.length() )
        {
            Path tmp_file = Files.createTempFile( "webin-cli", "download" );
            WebinCliReporter.writeToConsole( Severity.INFO, String.format( "Downloading latest version of Webin-CLI as %s", file.toPath() ) );
            long downloaded = download( new URL( info.assets.get( 0 ).browser_download_url ), tmp_file );
        
            if( ra.size != downloaded )
                throw WebinCliException.createSystemError( "Wrong download size" );
        
            if( ra.size != tmp_file.toFile().length() )
                throw WebinCliException.createSystemError( "Wrong file size" );
            
            if( !file.exists() || ra.size != file.length() )
            {
                try
                {
                    Files.move( tmp_file, file.toPath(), StandardCopyOption.REPLACE_EXISTING );
                } catch( IOException ioe )
                {
                    throw WebinCliException.createSystemError( String.format( "Unable to copy downloaded file %s into classpath destination folder as %s", tmp_file, file.getPath() ) );
                }
            }
        }
        

        try( URLClassLoader cl = new URLClassLoader( new URL[] { file.toURI().toURL() }, null ) )
        {
            WebinCliReporter.writeToConsole( Severity.INFO, "Using latest Webin-CLI version: " + file.getPath() );
            return (int) invokeMain( cl, args );
        }
    }
    

    public static void
    main( String[] args ) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, InterruptedException, ScriptException, URISyntaxException
    {
        VersionManager vm = new VersionManager();
        System.exit( vm.launchLatest( args ) );
    }
}
