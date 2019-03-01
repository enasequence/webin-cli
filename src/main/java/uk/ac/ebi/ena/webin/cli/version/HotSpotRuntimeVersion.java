
/*
 * Copyright 2018-2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class 
HotSpotRuntimeVersion 
{
    //HOTSPOT:
    //java.vm.name=Java HotSpot(TM) 64-Bit Server VM
    //java.runtime.version=11.0.1+13-LTS
    //java.version=11.0.1
    //java.vendor.version=18.9
            
    //J9
    //java.version=1.5.0
    //java.vm.vendor=IBM Corporation
    //java.vm.name=IBM J9 VM
    //java.runtime.version=pwi32dev-20061002a (SR3)
    //
    
    //OPEN JDK
    //java.runtime.name=OpenJDK Runtime Environment
    //java.vm.vendor=Sun Microsystems Inc.
    //java.vm.name=OpenJDK 64-Bit Server VM
    //java.runtime.version=1.7.0-internal-dlila_2010_10_21_13_54-b00
    //java.version=1.7.0-internal
    //java.vendor=Oracle Corporation
    
    
    private static final int FROM_MAJOR    = 1;
    private static final int FROM_MINOR    = 8;
    private static final int FROM_SECURITY = 0;
    private static final int FROM_BUILD    = 92; //151
    
    public static class 
    VersionInfo implements Comparable<VersionInfo>
    {
        final public Integer major;
        final public Integer minor;
        final public Integer security;
        
        final public Integer build;
        //OpenJDK Runtime Environment
        //

        
        public
        VersionInfo( int major, int minor, int security, int build )
        {
            this.major    = major;
            this.minor    = minor;
            this.security = security;
            
            this.build    = build;
        }
        
        
        public String
        toString()
        {
            return String.format( "version %d.%d.%d, build %d", major, minor, security, build );
        }

     
        @Override public int 
        compareTo( VersionInfo o )
        {
            return 0 == major.compareTo( o.major ) ? ( 0 == minor.compareTo( o.minor ) ? 0 == security.compareTo( o.security ) ? build.compareTo( o.build ) 
                                                                                                                               : security.compareTo( o.security )
                                                                                       : minor.compareTo( o.minor ) ) 
                                                   : major.compareTo( o.major );
        }
    }
    

    VersionInfo 
    getVersion( String java_version )
    {
        //1.8.0_172-b11
        //11.0.1+13-LTS
        Pattern vp = Pattern.compile( "^(\\d+)\\.(\\d+)\\.(\\d+)[^\\d](\\d+)[^\\d].*$" );
        Matcher m  = vp.matcher( java_version );
        if( m.find() )
        {
            try
            {
                return new VersionInfo( Integer.valueOf( m.group( 1 ) ),
                                        Integer.valueOf( m.group( 2 ) ),
                                        Integer.valueOf( m.group( 3 ) ),
                                        Integer.valueOf( m.group( 4 ) ) );
            } catch( NumberFormatException nfe )
            {
                return null;
            }
        } else
        {
            return null;
        }
    }
    
    
    public boolean
    isHotSpot()
    {
        String vendor = System.getProperty( "java.vm.name" );
        return  vendor.startsWith( "Java HotSpot(TM)" );
    }
    
    
    public VersionInfo 
    getCurrentVersion()
    {
        return isHotSpot() ? getVersion( System.getProperty( "java.runtime.version" ) ) : null;
    }
    
    
    public VersionInfo
    getMinVersion()
    {   
        return new VersionInfo( FROM_MAJOR, FROM_MINOR, FROM_SECURITY, FROM_BUILD );
    }
    
    
    public boolean
    isComplient()
    {
        return getMinVersion().compareTo( getCurrentVersion() ) <= 0;
    }
}
