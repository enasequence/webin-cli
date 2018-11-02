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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import uk.ac.ebi.embl.api.entry.feature.FeatureFactory;
import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile.FileType;
import uk.ac.ebi.ena.WebinCliTestUtils;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.study.Study;

public class TranscriptomeAssemblyValidationTest {

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


	private TranscriptomeAssemblyWebinCli prepareTranscriptomAssemblyWebinCli(File inputDir) {
		return prepareTranscriptomAssemblyWebinCli(getDefaultStudy(), getDefaultSample(),getDefaultSourceFeature(), inputDir);
	}

	private TranscriptomeAssemblyWebinCli prepareTranscriptomAssemblyWebinCli(Study study, Sample sample,SourceFeature source, File inputDir) {
		TranscriptomeAssemblyWebinCli cli = new TranscriptomeAssemblyWebinCli();
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
    @Test
    public void
    testTranscriptomeFileValidation_ValidFasta() throws Exception
    {
    	URL url = GenomeAssemblyValidationTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/transcriptome/simple_fasta/transcriptome.manifest" );
		File manifestFile = new File( url.getFile() );
		File inputDir = manifestFile.getParentFile();

		TranscriptomeAssemblyWebinCli validator = prepareTranscriptomAssemblyWebinCli(inputDir);

		try {
			validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));
		}
		finally {
			Assert.assertTrue(!validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).isEmpty());
			validator.validate();
		}
		
    }
    
}