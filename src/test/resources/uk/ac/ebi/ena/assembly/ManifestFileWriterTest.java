package uk.ac.ebi.ena.assembly;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import org.junit.Test;

import uk.ac.ebi.ena.manifest.ManifestFileReader;
import uk.ac.ebi.ena.manifest.ManifestFileWriter;

public class ManifestFileWriterTest 
{
 	@Test
	public void testValidateValidManifest() throws Exception 
	{
		File inputFile = null;
		File outputFile = null;

		ManifestFileWriter writer = new ManifestFileWriter(true);
		URL inUrl = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource("uk/ac/ebi/ena/assembly/manifestwithFastaOnly.txt");
		if (inUrl != null) 
		{
			inputFile = new File(inUrl.getPath().replaceAll("%20", " "));
		}
		URL outUrl = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource("uk/ac/ebi/ena/assembly/manifestwithFastaOnly.out");
		if (outUrl != null) 
		{
			outputFile = new File(outUrl.getPath().replaceAll("%20", " "));
			if(!outputFile.exists())
				outputFile.createNewFile();
			
		}
		ManifestFileReader reader= new ManifestFileReader();
		reader.read(inputFile.getAbsolutePath());
		writer.write(outputFile, reader.getManifestFileObjects());
		StringBuilder fileContent = new StringBuilder();
		try (BufferedReader r = Files.newBufferedReader(outputFile.toPath())) {
			  r.lines().forEach(l->fileContent.append(l));
			}
		assertEquals("FASTA	valid_fasta.txt	null", fileContent.toString());
	}
}
