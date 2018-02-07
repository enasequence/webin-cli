package uk.ac.ebi.ena.assembly;

import static org.junit.Assert.assertEquals;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import uk.ac.ebi.ena.manifest.ManifestFileReader;

public class GenomeAssemblyValidatorTest {
	@Test
	public void testAssemblyWithnoInfo() throws Exception {
		String fileName=null;
		URL url = GenomeAssemblyValidatorTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithFastaOnly.txt");
		if (url != null)
			fileName = url.getPath().replaceAll("%20", " ");
		ManifestFileReader reader= new ManifestFileReader();
		reader.read(fileName);
		List<String> locuTagsList = new ArrayList<>();
		GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli(reader, locuTagsList, true);
		int i= validator.validate();
		assertEquals(2, i);
	}

	@Test
	public void testAssemblywithOnlyInvalidInfo() throws Exception {
		String fileName=null;
		URL url = GenomeAssemblyValidatorTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithAssemblyinfoOnly.txt");
		if (url != null)
			fileName = url.getPath().replaceAll("%20", " ");
		ManifestFileReader reader= new ManifestFileReader();
		reader.read(fileName);
		List<String> locuTagsList = new ArrayList<>();
		GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli(reader, locuTagsList, true);
		int i= validator.validate();
		assertEquals(0, i);
	}

	@Test
	public void testAssemblyFastaInfo() throws Exception {
		String manifestFileName=null;
		URL manifestUrl = GenomeAssemblyValidatorTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithFastaInfo.txt");
		if (manifestUrl != null)
			manifestFileName = manifestUrl.getPath().replaceAll("%20", " ");
		ManifestFileReader reader= new ManifestFileReader();
		reader.read(manifestFileName);
		List<String> locuTagsList = new ArrayList<>();
		GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli(reader, locuTagsList, true);
		int i= validator.validate();
		assertEquals(0, i);
	}

	@Test
	public void testAssemblyFlatFileInfo() throws Exception	{
		String manifestFileName=null;
		URL manifestUrl = GenomeAssemblyValidatorTest.class.getClassLoader().getResource("uk/ac/ebi/ena/assembly/manifestwithFlatFileInfo.txt");
		if (manifestUrl != null)
			manifestFileName = manifestUrl.getPath().replaceAll("%20", " ");
		ManifestFileReader reader= new ManifestFileReader();
		reader.read(manifestFileName);
		List<String> locuTagsList = new ArrayList<>();
		GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli(reader, locuTagsList, true);
		int i= validator.validate();
		assertEquals(0, i);
	}

	@Test
	public void testAssemblywithUnlocalisedList() throws Exception	{
		String manifestFileName=null;
		URL manifestUrl = GenomeAssemblyValidatorTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithUnlocalisedListInfo.txt");
		if (manifestUrl != null)
			manifestFileName = manifestUrl.getPath().replaceAll("%20", " ");
		ManifestFileReader reader= new ManifestFileReader();
		reader.read(manifestFileName);
		List<String> locuTagsList = new ArrayList<>();
		GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli(reader, locuTagsList, true);
		int i= validator.validate();
		assertEquals(0, i);
	}
	
	@Test
	public void testAssemblywithAGP() throws Exception {
		String manifestFileName=null;
		URL manifestUrl = GenomeAssemblyValidatorTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithFastaAGPinfo.txt");
		if (manifestUrl != null)
			manifestFileName = manifestUrl.getPath().replaceAll("%20", " ");
		ManifestFileReader reader= new ManifestFileReader();
		reader.read(manifestFileName);
		List<String> locuTagsList = new ArrayList<>();
		GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli(reader, locuTagsList, true);
		int i= validator.validate();
		assertEquals(0, i);
	}
	
	@Test
	public void testAssemblywithChromosomeAGP() throws Exception {
		String manifestFileName=null;
		URL manifestUrl = GenomeAssemblyValidatorTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithChromosomeFastaAGPinfo.txt");
		if (manifestUrl != null)
			manifestFileName = manifestUrl.getPath().replaceAll("%20", " ");
		ManifestFileReader reader= new ManifestFileReader();
		reader.read(manifestFileName);
		List<String> locuTagsList = new ArrayList<>();
		GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli(reader, locuTagsList, true);
		int i= validator.validate();
		assertEquals(0, i);
	}
}
