package uk.ac.ebi.ena.manifest;

import static org.junit.Assert.assertTrue;
import java.net.URL;
import org.junit.Test;
import uk.ac.ebi.embl.api.validation.ValidationPlanResult;
import uk.ac.ebi.ena.manifest.ManifestFileReader;

public class ManifestFileReaderTest {
	
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
