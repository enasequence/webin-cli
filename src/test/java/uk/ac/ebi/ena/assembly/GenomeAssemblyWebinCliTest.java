package uk.ac.ebi.ena.assembly;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.ena.manifest.ManifestFileReader;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.study.Study;

public class GenomeAssemblyWebinCliTest {
	@Before public void
	before()
	{
		Locale.setDefault( Locale.UK );
		//-Duser.country=US -Duser.language=en
	}
	
	@Test
	public void testAssemblyWithnoInfo() throws Exception {
		String fileName=null;
		URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithFastaOnly.txt");
		if (url != null)
			fileName = url.getPath().replaceAll("%20", " ");
		ManifestFileReader reader= new ManifestFileReader();
		reader.read(fileName);
		Sample sample = new Sample();
		sample.setOrganism("Quercus robur");
		Study study = new Study();
		GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli(reader, sample,study,null,true);
		validator.setReportsDir( new File(fileName).getParent() );
		Assert.assertTrue( validator.validate() );
	}

	@Test
	public void testAssemblywithOnlyInvalidInfo() throws Exception {
		String fileName=null;
		URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithAssemblyinfoOnly.txt");
		if (url != null)
			fileName = url.getPath().replaceAll("%20", " ");
		ManifestFileReader reader= new ManifestFileReader();
		reader.read(fileName);
		Sample sample = new Sample();
		sample.setOrganism("Quercus robur");
		Study study = new Study();
		GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli(reader, sample,study,null,true);
		validator.setReportsDir( new File(fileName).getParent() );
		Assert.assertTrue( validator.validate() );
	}

	@Test
	public void testAssemblyFastaInfo() throws Exception {
		String manifestFileName=null;
		URL manifestUrl = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithFastaInfo.txt");
		if (manifestUrl != null)
			manifestFileName = manifestUrl.getPath().replaceAll("%20", " ");
		ManifestFileReader reader= new ManifestFileReader();
		reader.read(manifestFileName);
		Sample sample = new Sample();
		sample.setOrganism("Quercus robur");
		Study study = new Study();
		GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli(reader, sample,study,null,true);
		validator.setReportsDir( new File(manifestFileName).getParent() );
		Assert.assertTrue( validator.validate() );
	}

	@Test
	public void testAssemblyFlatFileInfo() throws Exception	{
		String manifestFileName=null;
		URL manifestUrl = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource("uk/ac/ebi/ena/assembly/manifestwithFlatFileInfo.txt");
		if (manifestUrl != null)
			manifestFileName = manifestUrl.getPath().replaceAll("%20", " ");
		ManifestFileReader reader= new ManifestFileReader();
		reader.read(manifestFileName);
		List<String> locusTagsList = new ArrayList<>();
		locusTagsList.add("SPLC1");
		Sample sample = new Sample();
		sample.setOrganism("Quercus robur");
		Study study = new Study();
		study.setLocusTagsList(locusTagsList);
		GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli(reader, sample,study,null, true);
		validator.setReportsDir( new File(manifestFileName).getParent() );
		Assert.assertTrue( validator.validate() );
	}

	@Test
	public void testAssemblywithUnlocalisedList() throws Exception	{
		String manifestFileName=null;
		URL manifestUrl = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithUnlocalisedListInfo.txt");
		if (manifestUrl != null)
			manifestFileName = manifestUrl.getPath().replaceAll("%20", " ");
		ManifestFileReader reader= new ManifestFileReader();
		reader.read(manifestFileName);
		Sample sample = new Sample();
		sample.setOrganism("Quercus robur");
		Study study = new Study();
		GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli(reader, sample,study,null,true);
		validator.setReportsDir( new File(manifestFileName).getParent() );
		Assert.assertTrue( validator.validate() );
	}
	
	@Test
	public void testAssemblywithAGP() throws Exception {
		String manifestFileName=null;
		URL manifestUrl = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithFastaAGPinfo.txt");
		if (manifestUrl != null)
			manifestFileName = manifestUrl.getPath().replaceAll("%20", " ");
		ManifestFileReader reader= new ManifestFileReader();
		reader.read(manifestFileName);
		Sample sample = new Sample();
		sample.setOrganism("Quercus robur");
		Study study = new Study();
		GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli(reader, sample,study,null,true);
		validator.setReportsDir( new File(manifestFileName).getParent() );
		Assert.assertTrue( validator.validate() );
	}
	
	@Test
	public void testAssemblywithChromosomeAGP() throws Exception {
		String manifestFileName=null;
		URL manifestUrl = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithChromosomeFastaAGPinfo.txt");
		if (manifestUrl != null)
			manifestFileName = manifestUrl.getPath().replaceAll("%20", " ");
		ManifestFileReader reader= new ManifestFileReader();
		reader.read(manifestFileName);
		Sample sample = new Sample();
		sample.setOrganism("Quercus robur");
		Study study = new Study();
		GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli(reader, sample,study,null,true);
		validator.setReportsDir( new File(manifestFileName).getParent() );
		Assert.assertTrue( validator.validate() );
	}
	

	@Test public void 
    testFastaNoValidEntries() throws Exception 
    {
        GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli( new ManifestFileReader(), new Sample(), new Study(), null, true );
        //TODO remove
        validator.setReportsDir( new File( GenomeAssemblyWebinCliTest.class
                .getClassLoader()
                .getResource( "uk/ac/ebi/ena/assembly/genome/ERZ480053/PYO97_7.fa.gz" )
                .getFile() ).getParent() );
        //validator.getParameters().setOutputDir( createOutputFolder() );
        Assert.assertTrue( !validator.validateFastaFiles( validator.getValidationProperties(), 
                                                          Arrays.asList( new File( GenomeAssemblyWebinCliTest.class
                                                                                              .getClassLoader()
                                                                                              .getResource( "uk/ac/ebi/ena/assembly/genome/ERZ480053/PYO97_7.fa.gz" )
                                                                                              .getFile() ) ) ) );
    }

    @Test public void 
    testChromosomeListNoValidEntries() throws Exception 
    {
        GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli( new ManifestFileReader(), new Sample(), new Study(), null, true );
        //TODO remove
        validator.setReportsDir( new File( GenomeAssemblyWebinCliTest.class
                .getClassLoader()
                .getResource( "uk/ac/ebi/ena/assembly/genome/ERZ496213/RUG553.fa.chromlist.gz" )
                .getFile() ).getParent() );
        //validator.getParameters().setOutputDir( createOutputFolder() );
        Assert.assertTrue( !validator.validateChromosomeList( validator.getValidationProperties(), 
                                                              new File( GenomeAssemblyWebinCliTest.class
                                                                                                  .getClassLoader()
                                                                                                  .getResource( "uk/ac/ebi/ena/assembly/genome/ERZ496213/RUG553.fa.chromlist.gz" )
                                                                                                  .getFile() ) ) );
    }

    
    private File
    createOutputFolder() throws IOException
    {
        File output = File.createTempFile( "test", "test" );
        Assert.assertTrue( output.delete() );
        Assert.assertTrue( output.mkdirs() );
        return output;
    }

    
    @Test
	public void testAssemblywithInvalidAGP() throws Exception {
		String manifestFileName=null;
		URL manifestUrl = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithFastaInvalidAGPinfo.txt");
		if (manifestUrl != null)
			manifestFileName = manifestUrl.getPath().replaceAll("%20", " ");
		ManifestFileReader reader= new ManifestFileReader();
		reader.read(manifestFileName);
		Sample sample = new Sample();
		sample.setOrganism("Quercus robur");
		Study study = new Study();
		GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli(reader, sample,study,null,true);
		validator.setReportsDir(new File(manifestFileName).getParent());
		Assert.assertTrue( !validator.validate() );
	}
	
	@Test
	public void testAssemblywithFlatfileandGP() throws Exception {
		String manifestFileName=null;
		URL manifestUrl = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithFlatfilevalidAGPinfo.txt");
		if (manifestUrl != null)
			manifestFileName = manifestUrl.getPath().replaceAll("%20", " ");
		ManifestFileReader reader= new ManifestFileReader();
		reader.read(manifestFileName);
		Sample sample = new Sample();
		sample.setOrganism("Quercus robur");
		Study study = new Study();
		GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli(reader, sample,study,null,true);
		validator.setReportsDir(new File(manifestFileName).getParent());
		Assert.assertTrue( validator.validate() );
	}
	
	@Test
	public void testAssemblywithSequencelessChromosomeAGP() throws Exception {
		String manifestFileName=null;
		URL manifestUrl = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithSequencelessChromosomeAGPinfo.txt");
		if (manifestUrl != null)
			manifestFileName = manifestUrl.getPath().replaceAll("%20", " ");
		ManifestFileReader reader= new ManifestFileReader();
		reader.read(manifestFileName);
		Sample sample = new Sample();
		sample.setOrganism("Quercus robur");
		Study study = new Study();
		GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli(reader, sample,study,null,true);
		validator.setReportsDir( new File(manifestFileName).getParent() );
		Assert.assertTrue(!validator.validate() );
	}
}
