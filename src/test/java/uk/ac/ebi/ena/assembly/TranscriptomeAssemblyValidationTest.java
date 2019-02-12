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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import uk.ac.ebi.embl.api.entry.feature.FeatureFactory;
import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile.FileType;
import uk.ac.ebi.ena.WebinCliTestUtils;
import uk.ac.ebi.ena.entity.Sample;
import uk.ac.ebi.ena.entity.Study;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TranscriptomeAssemblyValidationTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void
    before() {
        Locale.setDefault(Locale.UK);
    }

    private static Sample getDefaultSample() {
        Sample sample = new Sample();
        sample.setOrganism("Quercus robur");
        return sample;
    }

    private static Study getDefaultStudy() {
        return new Study();
    }

    private static SourceFeature getDefaultSourceFeature() {
        SourceFeature source = new FeatureFactory().createSourceFeature();
        source.setScientificName("Micrococcus sp. 5");
        return source;
    }

    private TranscriptomeAssemblyWebinCli prepareTranscriptomAssemblyWebinCli() {
        URL url = TranscriptomeAssemblyValidationTest.class.getClassLoader().getResource("uk/ac/ebi/ena/transcriptome/valid_fasta.fasta.gz");
        return prepareTranscriptomAssemblyWebinCli(new File(url.getFile()).getParentFile());
    }

    private TranscriptomeAssemblyWebinCli prepareTranscriptomAssemblyWebinCli(File inputDir) {
        TranscriptomeAssemblyWebinCli cli = new TranscriptomeAssemblyWebinCli();
        cli.setTestMode(true);
        cli.setInputDir(inputDir);
        cli.setValidationDir(WebinCliTestUtils.createTempDir());
        cli.setSubmitDir(WebinCliTestUtils.createTempDir());
        cli.setFetchSample(false);
        cli.setFetchStudy(false);
        cli.setFetchSource(false);
        cli.setSample(getDefaultSample());
        cli.setSource(getDefaultSourceFeature());
        cli.setStudy(getDefaultStudy());
        return cli;
    }

    @Test
    public void
    testFasta() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                "NAME test\n" +
                        "FASTA valid_fasta.fasta.gz\n" +
                        "STUDY ERP000003\n" +
                        "SAMPLE SAMEA3692850\n" +
                        "ASSEMBLYNAME test\n" +
                        "PROGRAM test\n" +
                        "PLATFORM test").toFile();

        TranscriptomeAssemblyWebinCli validator = prepareTranscriptomAssemblyWebinCli();

        validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));

        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles().size()).isEqualTo(1);
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).size()).isOne();

        validator.validate();
    }
}

