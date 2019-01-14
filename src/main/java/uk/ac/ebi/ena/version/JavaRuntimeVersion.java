package uk.ac.ebi.ena.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class 
JavaRuntimeVersion 
{
    final static float FROM_VERSION = 1.8f;

    Float 
    getVersion( String java_version )
    {
        //1.8.0_172-b11
        //11.0.1+13-LTS
        Pattern vp = Pattern.compile( "^(\\d+\\.\\d+)\\.(\\d+)[^\\d].*$" );
        Matcher m  = vp.matcher( java_version );
        if( m.find() )
        {
            try
            {
                return Float.valueOf( m.group( 1 ) + m.group( 2 ) );
            } catch( NumberFormatException nfe )
            {
                return Float.NaN;
            }
        } else
        {
            return Float.NaN;
        }
    }
    
    
    public Float 
    getCurrentVersion()
    {
        return getVersion( System.getProperty( "java.runtime.version" ) );
    }
    
    
    public Float
    getMinVersion()
    {
        return FROM_VERSION;
    }
    
    
    public boolean
    isComplient()
    {
        return !Float.isNaN( getCurrentVersion() ) && getCurrentVersion() >= FROM_VERSION;
    }
}
