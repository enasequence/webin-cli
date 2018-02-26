package uk.ac.ebi.ena.manifest;

import static org.junit.Assert.assertTrue;
import java.io.File;
import java.net.URL;
import org.junit.Test;
import uk.ac.ebi.embl.api.validation.ValidationPlanResult;
import uk.ac.ebi.ena.assembly.GenomeAssemblyWebinCliTest;
import uk.ac.ebi.ena.manifest.ManifestFileValidator;

public class ManifestFileValidatorTest {

	@Test
	public void testValidateManifestwithoutInfo() throws Exception {
		String fileName = null;
		ManifestFileValidator validator = new ManifestFileValidator(true);
		URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource("uk/ac/ebi/ena/assembly/manifestwithFastaOnly.txt");
		if (url != null) 
		{
			fileName = url.getPath().replaceAll("%20", " ");
		}
		assertTrue(!validator.validate(new File(fileName),new File(fileName).getParent(),"genome"));//info must be given in manifest file
	}
	
	@Test
	public void testValidateValidManifest() throws Exception {
		String fileName = null;
		ManifestFileValidator validator = new ManifestFileValidator(true);
		URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource("uk/ac/ebi/ena/assembly/manifestwithAssemblyinfoOnly.txt");
		if (url != null) 
		{
			fileName = url.getPath().replaceAll("%20", " ");
		}
		assertTrue(validator.validate(new File(fileName),new File(fileName).getParent(),"genome"));
	}

	@Test
	public void testValidateInvalidManifest() throws Exception {
		String fileName = null;
		ManifestFileValidator validator = new ManifestFileValidator(true);
		URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource("uk/ac/ebi/ena/assembly/invalidManifestFileTypes.txt");
		if (url != null) 
		{
			fileName = url.getPath().replaceAll("%20", " ");
		}
		assertTrue(!validator.validate(new File(fileName),new File(fileName).getParent(),"genome"));
	}
}
