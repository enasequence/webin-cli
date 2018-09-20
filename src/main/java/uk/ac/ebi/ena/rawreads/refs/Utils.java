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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.samtools.reference.ReferenceSequenceFile;
import htsjdk.samtools.util.Log;

public class Utils {

	private static Log log = Log.getInstance(Utils.class);
	public static boolean quitOnMissingEOF = Boolean.parseBoolean(System.getProperty("debug.quit-on-missing-eof",
			"true"));


	public static void checkRefMD5(SAMSequenceDictionary d, ReferenceSequenceFile refFile, boolean checkExistingMD5,
			boolean failIfMD5Mismatch) throws NoSuchAlgorithmException {

		for (SAMSequenceRecord r : d.getSequences()) {
			ReferenceSequence sequence = refFile.getSequence(r.getSequenceName());
			if (!r.getAttributes().contains(SAMSequenceRecord.MD5_TAG)) {
				String md5 = calculateMD5String(sequence.getBases());
				r.setAttribute(SAMSequenceRecord.MD5_TAG, md5);
			} else {
				if (checkExistingMD5) {
					String existingMD5 = r.getAttribute(SAMSequenceRecord.MD5_TAG);
					String md5 = calculateMD5String(sequence.getBases());
					if (!md5.equals(existingMD5)) {

						String message = String.format("For sequence %s the md5 %s does not match the actual md5 %s.",
								r.getSequenceName(), existingMD5, md5);

						if (failIfMD5Mismatch)
							throw new RuntimeException(message);
						else
							log.warn(message);
					}
				}
			}
		}
	}

	public static String calculateMD5String(byte[] data) {
		return calculateMD5String(data, 0, data.length);
	}

	public static String calculateMD5String(byte[] data, int offset, int len) {
		byte[] digest = calculateMD5(data, offset, len);
		return String.format("%032x", new BigInteger(1, digest));
	}

	public static byte[] calculateMD5(byte[] data, int offset, int len) {
		MessageDigest md5_MessageDigest;
		try {
			md5_MessageDigest = MessageDigest.getInstance("MD5");
			md5_MessageDigest.reset();

			md5_MessageDigest.update(data, offset, len);
			return md5_MessageDigest.digest();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	
	public static int readInto(ByteBuffer buf, InputStream inputStream) throws IOException {
		int read = 0;
		while (buf.hasRemaining()) {
			int count = inputStream.read(buf.array(), buf.position(), buf.remaining());
			if (count < 0)
				throw new EOFException();
			read += count;
		}
		return read;
	}
	
	
	public static boolean isValidSequence(byte[] bases, int checkOnlyThisManyBases) {
		for (int i = 0; i < checkOnlyThisManyBases && i < bases.length; i++) {
			switch (bases[i]) {
			case 'A':
			case 'C':
			case 'G':
			case 'T':
			case 'U':
			case 'R':
			case 'Y':
			case 'S':
			case 'W':
			case 'K':
			case 'M':
			case 'B':
			case 'D':
			case 'H':
			case 'V':
			case 'N':
			case '.':
			case '-':
				break;

			default:
				return false;
			}
		}
		return true;
	}
	
	public static final byte upperCase(byte base) {
		return base >= 'a' ? (byte) (base - ('a' - 'A')) : base;
	}

	public static final byte[] upperCase(byte[] bases) {
		for (int i = 0; i < bases.length; i++)
			bases[i] = upperCase(bases[i]);
		return bases;
	}


}
