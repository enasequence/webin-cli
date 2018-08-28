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

package uk.ac.ebi.ena.manifest;

import static org.junit.Assert.assertTrue;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.junit.Test;
import uk.ac.ebi.embl.api.validation.ValidationPlanResult;
import uk.ac.ebi.ena.manifest.ManifestFileReader;

public class ManifestFileReaderTest {
	
    @Test public void 
    testValidManifestComments() throws Exception 
    {
        ManifestFileReader reader = new ManifestFileReader();
        ValidationPlanResult result = reader.read( Files.write( Files.createTempFile( "MANIFEST", "MANIFEST" ), 
                                                                "FASTA 123.fasta.gz\n#FASTQ-FASTQ\n \n\n\nINFO            info.file".getBytes( StandardCharsets.UTF_8 ), 
                                                                StandardOpenOption.TRUNCATE_EXISTING ).toString() );
        assertTrue(result.isValid());
    }
    
    @Test
	public void testValidManifest() throws Exception 
	{
		String fileName = null;
		ManifestFileReader reader = new ManifestFileReader();
		URL url = ManifestFileReaderTest.class.getClassLoader().getResource("uk/ac/ebi/ena/assembly/manifestwithFastaOnly.txt");
		if (url != null) {
			fileName = url.getPath().replaceAll("%20", " ");
		}
		ValidationPlanResult result = reader.read(fileName);
		assertTrue(result.isValid());
	}

	@Test
	public void testInvalidManifest() throws Exception
	{
		String fileName = null;
		ManifestFileReader reader = new ManifestFileReader();
		URL url = ManifestFileReaderTest.class.getClassLoader().getResource("uk/ac/ebi/ena/assembly/invalidManifest.txt");
		if (url != null) {
			fileName = url.getPath().replaceAll("%20", " ");
		}
		ValidationPlanResult result = reader.read(fileName);
		assertTrue(!result.isValid());
	}

}
