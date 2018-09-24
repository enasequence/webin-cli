/*******************************************************************************
 * Copyright 2013 EMBL-EBI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package uk.ac.ebi.ena.rawreads.refs;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.cram.io.InputStreamUtils;
import htsjdk.samtools.cram.ref.CRAMReferenceSource;
import htsjdk.samtools.util.IOUtil;
import htsjdk.samtools.util.Log;



/**
 * A central class for automated discovery of reference sequences. The algorithm
 * is expected similar to that of samtools:
 * <ul>
 * <li>Search in memory cache by sequence name.</li>
 * <li>Use local fasta file is supplied as a reference file and cache the found
 * sequence in memory.</li>
 * <li>Try REF_CACHE env variable.</li>
 * <li>Try all entries in REF_PATH. The default value is the EBI reference
 * service.</li>
 * <li>Try @SQ:UR as a URL for a fasta file with the fasta index next to
 * it.</li>
 * </ul>
 * 
 * @author vadim
 */
public class ENAReferenceSource implements CRAMReferenceSource {
	private static final int REF_BASES_TO_CHECK_FOR_SANITY = 1000;
	private static final Pattern chrPattern = Pattern.compile("chr.*", Pattern.CASE_INSENSITIVE);
//	private static String REF_CACHE = System.getenv("REF_CACHE");
	private static String REF_PATH = System.getenv("REF_PATH");
	private List<PathPattern> refPatterns = new ArrayList<PathPattern>();
	private List<PathPattern> cachePatterns = new ArrayList<PathPattern>();
	
	Map<String, Integer> download_map = new HashMap<>();
	private AtomicInteger download_counter = new AtomicInteger();
	private AtomicLong    download_sz = new AtomicLong();
	private AtomicInteger memory_hit_counter = new AtomicInteger();
	private AtomicLong    total_spent = new AtomicLong();
	
	{
		if (REF_PATH == null)
			REF_PATH = "https://www.ebi.ac.uk/ena/cram/md5/%s";

//		if( REF_CACHE != null )
//			cachePatterns.add( new PathPattern( REF_CACHE ) );
		
		for (String s : REF_PATH.split("(?i)(?<!(https?|ftp)):")) {
			refPatterns.add(new PathPattern(s));
		}

	}
//	static class Log 
//	{
//	    public void debug( String s ) { System.out.println( s ); }
//	    public void warn( String s ) { debug( s ); }
//	    public void error( String s ) { debug( s ); }
//	    public void info( String s ) { debug( s ); }
//	}
	private static Log log = Log.getInstance(ENAReferenceSource.class);
	private int downloadTriesBeforeFailing = 2;

	/*
	 * In-memory cache of ref bases by sequence name. Garbage collector will
	 * automatically clean it if memory is low.
	 */
	private Map<String, Reference<byte[]>> cacheW = new ConcurrentHashMap<String, Reference<byte[]>>();

	public 
	ENAReferenceSource() 
	{
	    ;
	}

	
	public
	ENAReferenceSource( String... cachePatterns )
	{
	    this.cachePatterns.addAll( Arrays.stream( cachePatterns ).map( e -> new PathPattern( e ) ).collect( Collectors.toList() ) );
	}
	

	public void 
	clearCache()
	{
		cacheW.clear();
	}

	
    private byte[] 
    findInCache( String name )
    {
        Reference<byte[]> r = cacheW.get( name );
        if( r != null )
        {
            byte[] bytes = r.get();
            if( bytes != null )
                return bytes;
        }
        return null;
    }

    
    @Override
    public synchronized byte[] 
    getReferenceBases( SAMSequenceRecord record, boolean tryNameVariants )
    {
        byte[] bases = findBases( record, tryNameVariants );
        if( bases == null )
            return null;

        String md5 = record.getAttribute( SAMSequenceRecord.MD5_TAG );
        if( md5 == null )
        {
            md5 = Utils.calculateMD5String( bases );
            record.setAttribute( SAMSequenceRecord.MD5_TAG, md5 );
        }

        cacheW.put( md5, new SoftReference<byte[]>( bases ) );

        if( !cachePatterns.isEmpty() )
            addToRefCache( md5, bases );

        return bases;
    }
    
    
	private static byte[] 
    readBytesFromFile( File file, int offset, int len ) throws IOException
    {
        long size = file.length();
        if( size < offset || len < 0 )
        {
            log.warn( String.format( "Ref request is out of range: %s, size=%d, offset=%d, len=%d",
                                     file.getAbsolutePath(), 
                                     size, 
                                     offset, 
                                     len ) );
            return new byte[] {};
        }
        
        byte[] data = new byte[ (int) Math.min( size - offset, len ) ];
        FileInputStream fis = new FileInputStream( file );
        DataInputStream dis = new DataInputStream( fis );
        dis.skip( offset );
        dis.readFully( data );
        dis.close();
        return data;
    }

	
	public synchronized ReferenceRegion 
	getRegion( SAMSequenceRecord record, int start_1based, int endInclusive_1based ) throws IOException 
	{

        String md5 = record.getAttribute( SAMSequenceRecord.MD5_TAG );
        { // check cache by md5:
            if( md5 != null )
            {
                byte[] bases = findInCache( md5 );
                if( bases != null )
                {
                    log.debug( String.format( "% 6d Reference found in memory cache by md5: %s", memory_hit_counter.incrementAndGet(), md5 ) );
                    return ReferenceRegion.copyRegion( bases, 
                                                       record.getSequenceIndex(), 
                                                       record.getSequenceName(),
                                                       start_1based, 
                                                       endInclusive_1based );
                }
            }
        }

        byte[] bases = null;
        for( PathPattern pathPattern : cachePatterns )
        {
            File file = new File( pathPattern.format( md5 ) );
            if( file.exists() )
            {
                bases = readBytesFromFile( file, start_1based - 1, endInclusive_1based - start_1based + 1 );
                return new ReferenceRegion( bases, record.getSequenceIndex(), record.getSequenceName(), start_1based );
            }
        }

        { // try to fetch sequence by md5:
            if( md5 != null )
            {
                try
                {
                    bases = findBasesByMD5( md5 );
                } catch( Exception e )
                {
                    if( e instanceof RuntimeException )
                        throw (RuntimeException) e;
                    throw new RuntimeException( e );
                }
            }
            
            if( bases != null )
            {
                cacheW.put( md5, new SoftReference<byte[]>( bases ) );

                if( !cachePatterns.isEmpty() )
                    addToRefCache( md5, bases );

                return ReferenceRegion.copyRegion( bases, 
                                                   record.getSequenceIndex(), 
                                                   record.getSequenceName(),
                                                   start_1based, 
                                                   endInclusive_1based );
            }
        }
        return null;
	}

	
    private byte[] 
    findBases( SAMSequenceRecord record, boolean tryNameVariants )
    {

        String md5 = record.getAttribute( SAMSequenceRecord.MD5_TAG );
        { // check cache by md5:
            if( md5 != null )
            {
                byte[] bases = findInCache( md5 );
                if( bases != null )
                {
                    log.debug( String.format( "% 6d Reference found in memory cache by md5: %s", memory_hit_counter.incrementAndGet(), md5 ) );
                    return bases;
                }
            }
        }

        byte[] bases = null;

        { // try to fetch sequence by md5:
            if( md5 != null )
            {
                try
                {
                    bases = findBasesByMD5( md5 );
                } catch( Exception e )
                {
                    if( e instanceof RuntimeException )
                        throw (RuntimeException) e;
                    throw new RuntimeException( e );
                }
            }
            
            if( bases != null )
            {
                return bases;
            }
        }

        { // try @SQ:UR file location
            if( record.getAttribute( SAMSequenceRecord.URI_TAG ) != null )
            {
                ReferenceSequenceFromSeekable s = ReferenceSequenceFromSeekable.fromString( record.getAttribute( SAMSequenceRecord.URI_TAG ) );
                bases = s.getSubsequenceAt( record.getSequenceName(), 1, record.getSequenceLength() );
                Utils.upperCase( bases );
                return bases;
            }
        }
        return null;
    }

	/**
	 * @param path
	 * @return true if the path is a valid URL, false otherwise.
	 */
    private static boolean 
    isURL( String path )
    {
        try
        {
            URL url = new URL( path );
            return null != url;
        } catch( MalformedURLException e )
        {
            return false;
        }
    }

    
    private byte[] 
    loadFromPath( String path, String md5 ) throws IOException
    {
        if( isURL( path ) )
        {
            URL url = new URL( path );
            for( int i = 0; i < downloadTriesBeforeFailing; i++ )
            {
                InputStream is = url.openStream();
                if( is == null )
                    return null;

                if( !cachePatterns.isEmpty() )
                {
                    String localPath = addToRefCache( md5, is );
                    File file = new File( localPath );
                    if( file.length() > Integer.MAX_VALUE )
                        throw new RuntimeException( "The reference sequence is too long: " + md5 );

                    return readBytesFromFile( file, 0, (int) file.length() );
                }
    
                byte[] data = InputStreamUtils.readFully( is );
                is.close();

                if( confirmMD5( md5, data ) )
                {
                    // sanitise, Internet is a wild place:
                    if( Utils.isValidSequence( data, REF_BASES_TO_CHECK_FOR_SANITY ) )
                        return data;
                    else
                    {
                        // reject, it looks like garbage
                        log.error( "Downloaded sequence looks suspicous, rejected: " + url.toExternalForm() );
                        break;
                    }
                }
            }
        } else
        {
            File file = new File( path );
            if( file.exists() )
            {
                if( file.length() > Integer.MAX_VALUE )
                    throw new RuntimeException( "The reference sequence is too long: " + md5 );

                byte[] data = readBytesFromFile( file, 0, (int) file.length() );

                if( confirmMD5( md5, data ) )
                    return data;
                else
                    throw new RuntimeException( "MD5 mismatch for cached file: " + file.getAbsolutePath() );
            }
        }
        return null;
    }

    
    private byte[] 
    findBasesByMD5( String md5 ) throws MalformedURLException, IOException
    {
        long start = System.currentTimeMillis();
        for( PathPattern p : refPatterns )
        {
            String path = p.format( md5 );
            byte[] data = loadFromPath( path, md5 );
            if( data == null )
                continue;
            
            log.debug( String.format( "*% 5d Reference found at the location %s sz:%d, total: %d, spent: %d, total spent: %d, attempt %d",
                                      download_counter.incrementAndGet(),
                                      path,
                                      data.length,
                                      download_sz.addAndGet( data.length ),
                                      System.currentTimeMillis() - start,
                                      total_spent.addAndGet( System.currentTimeMillis() - start ),
                                      download_map.merge( md5, (Integer) 1, ( v1, v2 ) -> v1 + v2 ) ) );
            return data;
        }

        return null;
    }


    private void 
    addToRefCache( String md5, byte[] data )
    {
        for( PathPattern p : cachePatterns )
        {
            File cachedFile = new File( p.format( md5 ) );
            if( !cachedFile.exists() )
            {
                log.debug( String.format( "Adding to REF_CACHE: md5=%s, length=%d", md5, data.length ) );
                cachedFile.getParentFile().mkdirs();
                File tmpFile;
                try
                {
                    tmpFile = File.createTempFile( md5, ".tmp", cachedFile.getParentFile() );
                    FileOutputStream fos = new FileOutputStream( tmpFile );
                    fos.write( data );
                    fos.close();
                    if( !cachedFile.exists() )
                        tmpFile.renameTo( cachedFile );
                    else
                        tmpFile.delete();
    
                } catch( IOException e )
                {
                    throw new RuntimeException( e );
                }
            }
        }
    }

    
    private String 
    addToRefCache( String md5, InputStream stream )
    {
        for( PathPattern p : cachePatterns )
        {
            String localPath = p.format( md5 );
            File cachedFile = new File( localPath );
            if( !cachedFile.exists() )
            {
                log.info( String.format( "Adding to REF_CACHE sequence md5=%s", md5 ) );
                cachedFile.getParentFile().mkdirs();
                File tmpFile;
                try
                {
                    tmpFile = File.createTempFile( md5, ".tmp", cachedFile.getParentFile() );
                    FileOutputStream fos = new FileOutputStream( tmpFile );
                    IOUtil.copyStream( stream, fos );
                    fos.close();
                    if( !cachedFile.exists() )
                        tmpFile.renameTo( cachedFile );
                    else
                        tmpFile.delete();
                } catch( IOException e )
                {
                    throw new RuntimeException( e );
                }
            }
            return localPath;
        }
        return null;
    }
    

    private boolean 
    confirmMD5( String md5, byte[] data )
    {
        String downloadedMD5 = null;
        downloadedMD5 = Utils.calculateMD5String( data );
        if( md5.equals( downloadedMD5 ) )
        {
            return true;
        } else
        {
            String message = String.format( "Downloaded sequence is corrupt: requested md5=%s, received md5=%s", 
                                            md5,
                                            downloadedMD5 );
            log.error( message );
            return false;
        }
    }


    public int 
    getDownloadTriesBeforeFailing()
    {
        return downloadTriesBeforeFailing;
    }

    
    public void 
    setDownloadTriesBeforeFailing( int downloadTriesBeforeFailing )
    {
        this.downloadTriesBeforeFailing = downloadTriesBeforeFailing;
    }
}
