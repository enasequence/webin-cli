/*
 * Copyright 2018-2021 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class 
FileUtils 
{

	public static BufferedReader 
	getBufferedReader( File file ) throws IOException
	{
		if( file.getName().matches( "^.+\\.gz$" ) || file.getName().matches( "^.+\\.gzip$" ) ) 
		{
			GZIPInputStream gzip = new GZIPInputStream( new FileInputStream( file ) );
			return new BufferedReader( new InputStreamReader( gzip ) );
			
		} else if( file.getName().matches( "^.+\\.bz2$" ) || file.getName().matches( "^.+\\.bzip2$" ) ) 
		{
			BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream( new FileInputStream( file ) );
			return new BufferedReader( new InputStreamReader( bzIn ) );
			
		} else 
		{
			return new BufferedReader( new FileReader(file ) );
		}
	}

    public static String calculateDigest( String digestName, File file ) {
    	try {
			return calculateDigest(digestName, new FileInputStream( file ));
		} catch( IOException ex ) {
			throw WebinCliException.systemError( ex );
		}
    }

	public static String calculateDigest(String digestName, byte[] bytes) {
		return calculateDigest(digestName, new ByteArrayInputStream(bytes));
	}

	public static String calculateDigest(String digestName, InputStream is) {
		try {
			MessageDigest digest = MessageDigest.getInstance( digestName );
			byte[] buf = new byte[ 4096 ];
			int  read = 0;
			try( BufferedInputStream bis = new BufferedInputStream( is ) ) {
				while( ( read = bis.read( buf ) ) > 0 ) {
					digest.update(buf, 0, read);
				}

				byte[] message_digest = digest.digest();
				BigInteger value = new BigInteger( 1, message_digest );
				return String.format( String.format( "%%0%dx", message_digest.length << 1 ), value );
			}
		} catch( NoSuchAlgorithmException | IOException ex ) {
			throw WebinCliException.systemError( ex );
		}
	}

	public static boolean 
	emptyDirectory( File dir )
	{
		if (dir == null)
			return false;
	    if( dir.exists() )
	    {
	        File[] files = dir.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					emptyDirectory(file);
					file.delete();
				} else {
					file.delete();
				}
			}
	    }
	    return dir.listFiles().length == 0;
	}


}
