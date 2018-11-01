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
import java.util.Locale;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.ac.ebi.embl.api.entry.feature.FeatureFactory;
import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.check.file.FileValidationCheck;
import uk.ac.ebi.embl.api.validation.submission.Context;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile.FileType;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFiles;
import uk.ac.ebi.embl.api.validation.submission.SubmissionOptions;
import uk.ac.ebi.ena.WebinCliTestUtils;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class GenomeAssemblyValidationTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();
	@Before public void
	before()
	{
		Locale.setDefault( Locale.UK );
	}

	private static Sample getDefaultSample() {
		Sample sample = new Sample();
		sample.setOrganism( "Quercus robur" );
		return sample;
	}

	private static Study getDefaultStudy() {
		return new Study();
	}

	private static SourceFeature getDefaultSourceFeature()
	{
		SourceFeature source= new FeatureFactory().createSourceFeature();
		source.setScientificName("Micrococcus sp. 5");
		return source;
	}


	private File getDefaultInputDir() {
		URL url = GenomeAssemblyValidationTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/valid_fasta.txt" );
		return new File( url.getFile() ).getParentFile();
	}

	private GenomeAssemblyWebinCli prepareGenomeAssemblyWebinCli() {
		return prepareGenomeAssemblyWebinCli(getDefaultStudy(), getDefaultSample(),getDefaultSourceFeature(), getDefaultInputDir());
	}

	private GenomeAssemblyWebinCli prepareGenomeAssemblyWebinCli(File inputDir) {
		return prepareGenomeAssemblyWebinCli(getDefaultStudy(), getDefaultSample(),getDefaultSourceFeature(), inputDir);
	}

	private GenomeAssemblyWebinCli prepareGenomeAssemblyWebinCli(Study study, Sample sample,SourceFeature source, File inputDir) {
		GenomeAssemblyWebinCli cli = new GenomeAssemblyWebinCli();
		cli.setTestMode(true);
		cli.setInputDir( inputDir );
		cli.setValidationDir( WebinCliTestUtils.createTempDir() );
		cli.setSubmitDir( WebinCliTestUtils.createTempDir() );
		cli.setFetchSample(false);
		cli.setFetchStudy(false);
		cli.setFetchSource(false);
		cli.setSample(sample);
		cli.setSource(source);
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
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).isEmpty());
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
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).isEmpty());
			thrown.expect(ValidationEngineException.class);
			thrown.expectMessage(getmessage("fasta","invalid_fasta.txt", validator.getSubmissionOptions().reportDir.get()));
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
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).isEmpty());
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
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).isEmpty());
			thrown.expect(ValidationEngineException.class);
			thrown.expectMessage(getmessage("flatfile","invalid_flatfile.txt", validator.getSubmissionOptions().reportDir.get()));
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
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).isEmpty());
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.UNLOCALISED_LIST).isEmpty());
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
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).isEmpty());
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).isEmpty());
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
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).isEmpty());
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).isEmpty());
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.CHROMOSOME_LIST).isEmpty());
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
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).isEmpty());
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).isEmpty());
			thrown.expect(ValidationEngineException.class);
			thrown.expectMessage(getmessage("agp","invalid_agp.txt", validator.getSubmissionOptions().reportDir.get()));
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
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).isEmpty());
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).isEmpty());
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
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).isEmpty());
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).isEmpty());
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.CHROMOSOME_LIST).isEmpty());
			thrown.expect(ValidationEngineException.class);
			thrown.expectMessage("Sequenceless chromosomes are not allowed in assembly : IWGSC_CSS_6DL_SCAFF_3330719,IWGSC_CSS_6DL_SCAFF_3330717,IWGSC_CSS_6DL_SCAFF_3330716");
			Assert.assertFalse(validator.validate());
		}
	}

	//
	//

	@Test public void
	testGenomeFileValidation_Valid_ERZ449652() throws Exception
	{
		URL url = GenomeAssemblyValidationTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/genome/ERZ449652/ERZ449652.manifest" );
		File manifestFile = new File( url.getFile() );
		File inputDir = manifestFile.getParentFile();

		GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli(inputDir);

		try {
			validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));
		}
		finally {
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).isEmpty());
			Assert.assertTrue(validator.validate());
		}
	}

	@Test public void
	testGenomeFileValidation_Invalid_ERZ092580() throws Exception
	{
		URL url = GenomeAssemblyValidationTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/genome/ERZ092580/ERZ092580.manifest" );
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
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).isEmpty());
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).isEmpty());
			thrown.expect(ValidationEngineException.class);
			Assert.assertFalse(validator.validate());
		}
	}

	@Test public void
	testGenomeFileValidation_InvalidFasta_ERZ480053() throws Exception
	{

		File file =WebinCliTestUtils.getFile( "uk/ac/ebi/ena/assembly/genome/ERZ480053/PYO97_7.fa.gz");
		GenomeAssemblyWebinCli validator=getValidator(file, FileType.FASTA);
		thrown.expect(ValidationEngineException.class);
		thrown.expectMessage(getmessage("fasta",file.getName(), validator.getSubmissionOptions().reportDir.get()));
		validator.validateInternal();
	}

	@Test public void
	testGenomeFileValidation_InvalidChromosomeList_ERZ496213() throws Exception
	{
		File file=WebinCliTestUtils.getFile( "uk/ac/ebi/ena/assembly/genome/ERZ496213/RUG553.fa.chromlist.gz");
		GenomeAssemblyWebinCli validator=getValidator(file, FileType.CHROMOSOME_LIST);
		thrown.expect(ValidationEngineException.class);
		thrown.expectMessage(getmessage("chromosome_list",file.getName(), validator.getSubmissionOptions().reportDir.get()));
		validator.validateInternal();
	}
	

	private GenomeAssemblyWebinCli getValidator(File file,FileType fileType)

	{
		SubmissionOptions options = new SubmissionOptions();
		if(file!=null)
		{
			SubmissionFiles files = new SubmissionFiles();
			SubmissionFile SubmissionFile= new SubmissionFile(fileType,file);
			files.addFile(SubmissionFile);
			options.submissionFiles = Optional.of(files);
		}
		options.assemblyInfoEntry = Optional.of(new AssemblyInfoEntry());
		options.context =Optional.of(Context.genome);
		options.isFixMode =true;
		options.isRemote =true;
		options.source =Optional.of(getDefaultSourceFeature());
		GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli();
		validator.setTestMode( true );
		validator.setStudy( new Study() );
		options.reportDir=Optional.of(WebinCliTestUtils.createTempDir().getAbsolutePath());
		validator.setSubmitDir(WebinCliTestUtils.createTempDir());
		validator.setSubmissionOptions(options);
		return validator;
	}

	private String getmessage(String fileType,String fileName,String reportDir)
	{
		return fileType+" file validation failed : "+fileName+", Please see the error report: "+ reportDir+File.separator+fileName+".report";
	}
}
