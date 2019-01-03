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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import javax.script.ScriptException;

import uk.ac.ebi.ena.version.LatestRelease.GitHubReleaseAsset;
import uk.ac.ebi.ena.version.LatestRelease.GitHubReleaseInfo;

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
            throw new RuntimeException( "Unable to fetch information about latest release" );
        
        GitHubReleaseAsset ra = info.assets.get( 0 );
        
        if( !"uploaded".equals( ra.state ) )
            throw new RuntimeException( "Unable to fetch information about latest release" );
        
        File file = new File( ".", ra.name );
        
        if( findInClassPath( file ) )
            return (int) invokeMain( this.getClass().getClassLoader(), args );
        
        if( !file.exists() || ra.size != file.length() )
        {
            long downloaded = download( new URL( info.assets.get( 0 ).browser_download_url ), file.toPath() );
        
            if( ra.size != downloaded )
                throw new RuntimeException( "Wrong download size" );
        
            if( ra.size != file.length() )
                throw new RuntimeException( "Wrong file size" );
        }
        

        try( URLClassLoader cl = new URLClassLoader( new URL[] { file.toURI().toURL() }, null ) )
        {
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
