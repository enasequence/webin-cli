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

package uk.ac.ebi.ena.assembly;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.ena.WebinCliTestUtils;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

	public class GenomeAssemblyWebinCliTest {
	@Before public void
	before()
	{
		Locale.setDefault( Locale.UK );
	}

	public static Sample getDefaultSample() {
		Sample sample = new Sample();
		sample.setOrganism( "Quercus robur" );
		return sample;
	}

	public static Study getDefaultStudy() {
		return new Study();
	}

	private File getDefaultInputDir() {
		URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/valid_fasta.txt" );
		return new File( url.getFile() ).getParentFile();
	}

	private GenomeAssemblyWebinCli prepareGenomeAssemblyWebinCli() {
		return prepareGenomeAssemblyWebinCli(getDefaultStudy(), getDefaultSample(), getDefaultInputDir());
	}

	private GenomeAssemblyWebinCli prepareGenomeAssemblyWebinCli(File inputDir) {
		return prepareGenomeAssemblyWebinCli(getDefaultStudy(), getDefaultSample(), inputDir);
	}

	private GenomeAssemblyWebinCli prepareGenomeAssemblyWebinCli(Study study, Sample sample) {
		return prepareGenomeAssemblyWebinCli(study, sample, getDefaultInputDir());
	}

	private GenomeAssemblyWebinCli prepareGenomeAssemblyWebinCli(Study study, Sample sample, File inputDir) {
		GenomeAssemblyWebinCli cli = new GenomeAssemblyWebinCli();
		cli.setTestMode(true);
		cli.setInputDir( inputDir );
		cli.setValidationDir( WebinCliTestUtils.createOutputFolder() );
		cli.setSubmitDir( WebinCliTestUtils.createOutputFolder() );
		cli.setFetchSample(false);
		cli.setFetchStudy(false);
		cli.setSample(sample);
		cli.setStudy(study);
		return cli;
	}


	@Test public void
	testGenomeValidationValidFasta() throws Exception
	{
		File manifestFile = WebinCliTestUtils.createTempFile(false,
				"NAME\ttest\n" +
				"FASTA\tvalid_fasta.txt").toFile();

		GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();

		try {
			validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));
		}
		catch (WebinCliException ex) {
			Assert.assertTrue(ex.getMessage().startsWith("Invalid manifest file"));
		}
		finally {
			Assert.assertTrue(!validator.fastaFiles.isEmpty());
			Assert.assertTrue(validator.validate());
		}
	}

	@Test public void
	testGenomeFileValidation_InvalidFasta() throws Exception
	{
		File manifestFile = WebinCliTestUtils.createTempFile(false,
				"NAME\ttest\n" +
				"FASTA\tinvalid_fasta.txt").toFile();

		GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();

		try {
			validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));
		}
		catch (WebinCliException ex) {
			Assert.assertTrue(ex.getMessage().startsWith("Invalid manifest file"));
		}
		finally {
			Assert.assertTrue(!validator.fastaFiles.isEmpty());
			Assert.assertFalse(validator.validate());
		}
	}

	@Test public void
	testGenomeFileValidation_ValidFlatFile() throws Exception
	{
		File manifestFile = WebinCliTestUtils.createTempFile(false,
				"NAME\ttest\n" +
				"FLATFILE\tvalid_flatfile.txt").toFile();

		GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();

		try {
			validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));
		}
		catch (WebinCliException ex) {
			Assert.assertTrue(ex.getMessage().startsWith("Invalid manifest file"));
		}
		finally {
			Assert.assertTrue(!validator.flatFiles.isEmpty());
			Assert.assertTrue(validator.validate());
		}
	}

	@Test public void
	testGenomeFileValidation_InvalidFlatFile() throws Exception
	{
		File manifestFile = WebinCliTestUtils.createTempFile(false,
				"NAME\ttest\n" +
				"FLATFILE\tinvalid_flatfile.txt").toFile();

		GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();

		try {
			validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));
		}
		catch (WebinCliException ex) {
			Assert.assertTrue(ex.getMessage().startsWith("Invalid manifest file"));
		}
		finally {
			Assert.assertTrue(!validator.flatFiles.isEmpty());
			Assert.assertFalse(validator.validate());
		}
	}

	@Test public void
	testGenomeFileValidation_ValidFastaAndUnlocalisedList() throws Exception
	{
		File manifestFile = WebinCliTestUtils.createTempFile(false,
				"NAME\ttest\n" +
				"UNLOCALISED_LIST\tunlocalised_list.txt\n" +
				"FASTA\tvalid_fasta.txt").toFile();

		GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();

		try {
			validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));
		}
		catch (WebinCliException ex) {
			Assert.assertTrue(ex.getMessage().startsWith("Invalid manifest file"));
		}
		finally {
			Assert.assertTrue(!validator.fastaFiles.isEmpty());
			Assert.assertNotNull(validator.unlocalisedListFile);
			Assert.assertTrue(validator.validate());
		}
	}

	@Test public void
	testGenomeFileValidation_ValidFastaAndAgp() throws Exception
	{
		File manifestFile = WebinCliTestUtils.createTempFile(false,
				"NAME\ttest\n" +
				"AGP\tvalid_agp.txt\n" +
				"FASTA\tvalid_fasta.txt").toFile();

		GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();

		try {
			validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));
		}
		catch (WebinCliException ex) {
			Assert.assertTrue(ex.getMessage().startsWith("Invalid manifest file"));
		}
		finally {
			Assert.assertTrue(!validator.fastaFiles.isEmpty());
			Assert.assertTrue(!validator.agpFiles.isEmpty());
			Assert.assertTrue(validator.validate());
		}
	}

	@Test public void
	testGenomeFileValidation_ValidFastaAndAgpAndChromosomeList() throws Exception
	{
		File manifestFile = WebinCliTestUtils.createTempFile(false,
				"NAME\ttest\n" +
				"CHROMOSOME_LIST\tchromosome_list.txt\n" +
				"AGP\tvalid_agp.txt\n" +
				"FASTA\tvalid_fasta.txt").toFile();

		GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();

		try {
			validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));
		}
		catch (WebinCliException ex) {
			Assert.assertTrue(ex.getMessage().startsWith("Invalid manifest file"));
		}
		finally {
			Assert.assertTrue(!validator.fastaFiles.isEmpty());
			Assert.assertTrue(!validator.agpFiles.isEmpty());
			Assert.assertNotNull(validator.chromosomeListFile);
			Assert.assertTrue(validator.validate());
		}
	}

	@Test public void
	testGenomeFileValidation_ValidFastaInvalidAGP() throws Exception
	{
		File manifestFile = WebinCliTestUtils.createTempFile(false,
				"NAME\ttest\n" +
				"FASTA\tvalid_fasta.txt\n" +
				"AGP\tinvalid_agp.txt\n").toFile();

		GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();

		try {
			validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));
		}
		catch (WebinCliException ex) {
			Assert.assertTrue(ex.getMessage().startsWith("Invalid manifest file"));
		}
		finally {
			Assert.assertTrue(!validator.fastaFiles.isEmpty());
			Assert.assertTrue(!validator.agpFiles.isEmpty());
			Assert.assertFalse(validator.validate());
		}
	}

	@Test public void
	testGenomeFileValidation_ValidFlatFileAGP() throws Exception
	{
		File manifestFile = WebinCliTestUtils.createTempFile(false,
				"NAME\ttest\n" +
				"FLATFILE\tvalid_flatfileforAgp.txt\n" +
				"AGP\tvalid_flatfileagp.txt").toFile();

		GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();

		try {
			validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));
		}
		catch (WebinCliException ex) {
			Assert.assertTrue(ex.getMessage().startsWith("Invalid manifest file"));
		}
		finally {
			Assert.assertTrue(!validator.flatFiles.isEmpty());
			Assert.assertTrue(!validator.agpFiles.isEmpty());
			Assert.assertTrue(validator.validate());
		}
	}


	@Test public void
	testGenomeFileValidation_FastaAgpSequencelessChromosome() throws Exception
	{
		File manifestFile = WebinCliTestUtils.createTempFile(false,
				"NAME\ttest\n" +
				"FASTA\tvalid_fasta.txt\n" +
				"CHROMOSOME_LIST\tchromosome_list_sequenceless.txt\n" +
				"AGP\tvalid_agp.txt\n").toFile();

		GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();

		try {
			validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));
		}
		catch (WebinCliException ex) {
			Assert.assertTrue(ex.getMessage().startsWith("Invalid manifest file"));
		}
		finally {
			Assert.assertTrue(!validator.fastaFiles.isEmpty());
			Assert.assertTrue(!validator.agpFiles.isEmpty());
			Assert.assertNotNull(validator.chromosomeListFile);
			Assert.assertFalse(validator.validate());
		}
	}

	//
	//

	@Test public void
	testGenomeFileValidation_Valid_ERZ449652() throws Exception
	{
		URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/genome/ERZ449652/ERZ449652.manifest" );
		File manifestFile = new File( url.getFile() );
		File inputDir = manifestFile.getParentFile();

		GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli(inputDir);

		try {
			validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));
		}
		finally {
			Assert.assertTrue(!validator.flatFiles.isEmpty());
			Assert.assertTrue(validator.validate());
		}
	}

	@Test public void
	testGenomeFileValidation_Invalid_ERZ092580() throws Exception
	{
		URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/genome/ERZ092580/ERZ092580.manifest" );
		File manifestFile = new File( url.getFile() );
		File inputDir = manifestFile.getParentFile();

		GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli(inputDir);

		try {
			validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));
		}
		catch (WebinCliException ex) {
			Assert.assertTrue(ex.getMessage().startsWith("Invalid manifest file"));
		}
		finally {
			Assert.assertTrue(!validator.fastaFiles.isEmpty());
			Assert.assertTrue(!validator.agpFiles.isEmpty());
			Assert.assertFalse(validator.validate());
		}
	}


	//
	//


	@Test public void
	testGenomeFileValidation_InvalidFasta_ERZ480053() throws Exception
    {
        GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli();
        validator.setTestMode( true );
        validator.setStudy( new Study() );
        validator.setValidationDir( WebinCliTestUtils.createOutputFolder() );
        validator.setSubmitDir( WebinCliTestUtils.createOutputFolder() );
        Assert.assertTrue( !validator.validateFastaFiles( validator.getValidationProperties(), 
						  Arrays.asList( WebinCliTestUtils.getFile( "uk/ac/ebi/ena/assembly/genome/ERZ480053/PYO97_7.fa.gz" ))));
    }

	@Test public void
	testGenomeFileValidation_InvalidChromosomeList_ERZ496213() throws Exception
    {
        GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli();
		validator.setTestMode( true );
        validator.setStudy( new Study() );
        validator.setValidationDir( WebinCliTestUtils.createOutputFolder() );
        validator.setSubmitDir( WebinCliTestUtils.createOutputFolder() );
        Assert.assertTrue( !validator.validateChromosomeList( validator.getValidationProperties(),
				WebinCliTestUtils.getFile( "uk/ac/ebi/ena/assembly/genome/ERZ496213/RUG553.fa.chromlist.gz" )));
    }
}
