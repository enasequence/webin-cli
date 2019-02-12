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
import org.junit.Test;

import uk.ac.ebi.embl.api.entry.feature.FeatureFactory;
import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile.FileType;
import uk.ac.ebi.ena.WebinCliTestUtils;
import uk.ac.ebi.ena.entity.Sample;
import uk.ac.ebi.ena.entity.Study;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class GenomeAssemblyValidationTest {

    private final static String DEFAULT_MANIFEST_META_FIELDS =
            "STUDY 123\n" +
                    "SAMPLE 123\n" +
                    "COVERAGE 1 \n" +
                    "PROGRAM program\n" +
                    "PLATFORM Illumina\n" +
                    "NAME test\n";

    @Before
    public void
    before() {
        Locale.setDefault(Locale.UK);
    }

    private static Study getDefaultStudy() {
        return new Study();
    }

    private static Sample getDefaultSample() {
        Sample sample = new Sample();
        sample.setOrganism("Quercus robur");
        return sample;
    }

    private static SourceFeature getDefaultSourceFeature() {
        SourceFeature source = new FeatureFactory().createSourceFeature();
        source.setScientificName("Micrococcus sp. 5");
        return source;
    }

    private static Sample getHumanSample() {
        Sample sample = new Sample();
        sample.setOrganism("Homo sapiens");
        return sample;
    }

    private static SourceFeature getHumanSourceFeature() {
        SourceFeature source = new FeatureFactory().createSourceFeature();
        source.setScientificName("Homo sapiens");
        return source;
    }

    private GenomeAssemblyWebinCli prepareGenomeAssemblyWebinCli() {
        URL url = GenomeAssemblyValidationTest.class.getClassLoader().getResource("uk/ac/ebi/ena/assembly/valid_fasta.fasta.gz");
        return prepareGenomeAssemblyWebinCli(new File(url.getFile()).getParentFile());
    }

    private GenomeAssemblyWebinCli prepareGenomeAssemblyWebinCli(File inputDir) {
        GenomeAssemblyWebinCli cli = new GenomeAssemblyWebinCli();
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

    private GenomeAssemblyWebinCli assertThatManifestIsInvalid(File manifestFile, GenomeAssemblyWebinCli genomeAssemblyWebinCli) {
        GenomeAssemblyWebinCli validator = genomeAssemblyWebinCli;

        assertThatThrownBy(() -> {
            validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));
        }).isInstanceOf(WebinCliException.class)
                .hasMessageContaining("Invalid manifest file");

        return validator;
    }

    private void assertThatManifestErrorIs(GenomeAssemblyWebinCli validator, String messageKey) {
        assertThat(validator.getManifestReader().getValidationResult().getMessages().size()).isOne();
        validator.getManifestReader().getValidationResult().getMessages().forEach(e ->
                assertThat(e.getMessageKey()).isEqualTo(messageKey));
    }

    @Test
    public void
    testFasta() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                DEFAULT_MANIFEST_META_FIELDS +
                        "FASTA\tvalid_fasta.fasta.gz").toFile();

        GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();

        validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));

        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles().size()).isEqualTo(1);
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).size()).isOne();

        validator.validate();
    }

    @Test
    public void
    testFlatFile() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                DEFAULT_MANIFEST_META_FIELDS +
                        "FLATFILE\tvalid_flatfile.txt.gz").toFile();

        GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();

        validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));

        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles().size()).isEqualTo(1);
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).size()).isOne();

        validator.validate();
    }

    @Test
    public void
    testFastaAndAgp() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                DEFAULT_MANIFEST_META_FIELDS +
                        "AGP\tvalid_agp.agp.gz\n" +
                        "FASTA\tvalid_fasta.fasta.gz").toFile();

        GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();

        validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));

        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles().size()).isEqualTo(2);
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).size()).isOne();
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).size()).isOne();

        validator.validate();
    }

    @Test
    public void
    testFlatFileAndAgp() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                DEFAULT_MANIFEST_META_FIELDS +
                        "AGP\tvalid_agp.agp.gz\n" +
                        "FLATFILE\tvalid_flatfile.txt.gz").toFile();

        GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();

        validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));

        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles().size()).isEqualTo(2);
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).size()).isOne();
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).size()).isOne();

        validator.validate();
    }

    @Test
    public void
    testFastaAndAgpAndChromosomeList() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                DEFAULT_MANIFEST_META_FIELDS +
                        "CHROMOSOME_LIST\tvalid_chromosome_list.txt.gz\n" +
                        "AGP\tvalid_agp.agp.gz\n" +
                        "FASTA\tvalid_fasta.fasta.gz").toFile();

        GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();
        validator.setSample(getHumanSample());
        validator.setSource(getHumanSourceFeature());

        validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));

        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles().size()).isEqualTo(3);
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).size()).isOne();
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).size()).isOne();
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.CHROMOSOME_LIST).size()).isOne();

        validator.validate();
    }

    @Test
    public void
    testFlatFileAndAgpAndChromosomeList() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                DEFAULT_MANIFEST_META_FIELDS +
                        "CHROMOSOME_LIST\tvalid_chromosome_list.txt.gz\n" +
                        "AGP\tvalid_agp.agp.gz\n" +
                        "FLATFILE\tvalid_flatfile.txt.gz").toFile();

        GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();
        validator.setSample(getHumanSample());
        validator.setSource(getHumanSourceFeature());

        validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));

        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles().size()).isEqualTo(3);
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).size()).isOne();
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).size()).isOne();
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.CHROMOSOME_LIST).size()).isOne();

        validator.validate();
    }

    @Test
    public void
    testFastaAndAgpAndSequencelessChromosomeList() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                DEFAULT_MANIFEST_META_FIELDS +
                        "AGP\tvalid_agp.agp.gz\n" +
                        "FLATFILE\tvalid_flatfile.txt.gz\n" +
                        "CHROMOSOME_LIST\tinvalid_chromosome_list_sequenceless.txt.gz\n").toFile();

        GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();
        validator.setSample(getHumanSample());
        validator.setSource(getHumanSourceFeature());

        validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));

        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles().size()).isEqualTo(3);
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).size()).isOne();
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).size()).isOne();
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.CHROMOSOME_LIST).size()).isOne();

        assertThatThrownBy(() -> {
            validator.validate();
        }).isInstanceOf(WebinCliException.class)
                .hasMessageContaining("Sequenceless chromosomes are not allowed in assembly");
    }

    @Test
    public void
    testInvalidSuffix_Fasta() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                DEFAULT_MANIFEST_META_FIELDS +
                        "FASTA\tinvalid_fasta.fasta").toFile();

        GenomeAssemblyWebinCli validator = assertThatManifestIsInvalid(manifestFile, prepareGenomeAssemblyWebinCli());
        assertThatManifestErrorIs(validator, "MANIFEST_INVALID_FILE_SUFFIX");
    }

    @Test
    public void
    testInvalidSuffix_FlatFile() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                DEFAULT_MANIFEST_META_FIELDS +
                        "FLATFILE\tinvalid_flatfile.txt").toFile();

        GenomeAssemblyWebinCli validator = assertThatManifestIsInvalid(manifestFile, prepareGenomeAssemblyWebinCli());
        assertThatManifestErrorIs(validator, "MANIFEST_INVALID_FILE_SUFFIX");
    }

    @Test
    public void
    testInvalidFileSuffix_Agp() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                DEFAULT_MANIFEST_META_FIELDS +
                        "AGP\tinvalid_agp.agp\n" +
                        "FASTA\tvalid_fasta.fasta.gz").toFile();

        GenomeAssemblyWebinCli validator = assertThatManifestIsInvalid(manifestFile, prepareGenomeAssemblyWebinCli());
        assertThatManifestErrorIs(validator, "MANIFEST_INVALID_FILE_SUFFIX");
    }

    @Test
    public void
    testInvalidFile_Fasta() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                DEFAULT_MANIFEST_META_FIELDS +
                        "FASTA\tinvalid_fasta.fasta.gz").toFile();

        GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();

        validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));

        assertThatThrownBy(() -> {
            validator.validate();
        }).isInstanceOf(WebinCliException.class)
                .hasMessageContaining("fasta file validation failed");
    }

    @Test
    public void
    testInvalidFile_FlatFile() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                DEFAULT_MANIFEST_META_FIELDS +
                        "FLATFILE\tinvalid_flatfile.txt.gz").toFile();

        GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();

        validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));

        assertThatThrownBy(() -> {
            validator.validate();
        }).isInstanceOf(WebinCliException.class)
                .hasMessageContaining("flatfile file validation failed");
    }

    @Test
    public void
    testInvalidFile_Agp() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                DEFAULT_MANIFEST_META_FIELDS +
                        "FASTA\tvalid_fasta.fasta.gz\n" +
                        "AGP\tinvalid_agp.agp.gz").toFile();

        GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();

        validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));

        assertThatThrownBy(() -> {
            validator.validate();
        }).isInstanceOf(WebinCliException.class)
                .hasMessageContaining("agp file validation failed");
    }


    @Test
    public void
    testInvalidFileGroup_FastaAndUnlocalisedList() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                DEFAULT_MANIFEST_META_FIELDS +
                        "UNLOCALISED_LIST\tvalid_unlocalised_list.txt.gz\n" +
                        "FASTA\tvalid_fasta.fasta.gz").toFile();

        assertThatManifestIsInvalid(manifestFile, prepareGenomeAssemblyWebinCli());
    }

    @Test
    public void
    testInvalidFileGroup_FlatFileAndUnlocalisedList() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                DEFAULT_MANIFEST_META_FIELDS +
                        "UNLOCALISED_LIST\tvalid_unlocalised_list.txt.gz\n" +
                        "FLATFILE\tvalid_flatfile.txt.gz").toFile();

        GenomeAssemblyWebinCli validator = assertThatManifestIsInvalid(manifestFile, prepareGenomeAssemblyWebinCli());
        assertThatManifestErrorIs(validator, "MANIFEST_ERROR_INVALID_FILE_GROUP");
    }

    @Test
    public void
    testBinnedMetagenome_Fasta() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                DEFAULT_MANIFEST_META_FIELDS +
                        "ASSEMBLY_TYPE binned metagenome\n" +
                        "FASTA\tvalid_fasta.fasta.gz").toFile();

        GenomeAssemblyWebinCli validator = prepareGenomeAssemblyWebinCli();

        validator.init(WebinCliTestUtils.createWebinCliParameters(manifestFile, validator.getInputDir()));

        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles().size()).isEqualTo(1);
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).size()).isOne();

        validator.validate();
    }

    @Test
    public void
    testBinnedMetagenome_InvalidNoFasta() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                DEFAULT_MANIFEST_META_FIELDS +
                        "ASSEMBLY_TYPE binned metagenome\n").toFile();

        GenomeAssemblyWebinCli validator = assertThatManifestIsInvalid(manifestFile, prepareGenomeAssemblyWebinCli());
        assertThatManifestErrorIs(validator, "MANIFEST_ERROR_NO_DATA_FILES");
    }

    @Test
    public void
    testBinnedMetagenome_InvalidExtraFasta() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                DEFAULT_MANIFEST_META_FIELDS +
                        "ASSEMBLY_TYPE binned metagenome\n" +
                        "FASTA valid_fasta.fasta.gz\n" +
                        "FASTA valid_fasta.fasta.gz").toFile();

        GenomeAssemblyWebinCli validator = assertThatManifestIsInvalid(manifestFile, prepareGenomeAssemblyWebinCli());
        assertThatManifestErrorIs(validator, "MANIFEST_TOO_MANY_FIELDS");
    }

    @Test
    public void
    testBinnedMetagenome_InvalidExtraAgp() {
        File manifestFile = WebinCliTestUtils.createTempFile(false,
                DEFAULT_MANIFEST_META_FIELDS +
                        "ASSEMBLY_TYPE binned metagenome\n" +
                        "FASTA valid_fasta.fasta.gz\n" +
                        "AGP valid_agp.agp.gz").toFile();

        GenomeAssemblyWebinCli validator = assertThatManifestIsInvalid(manifestFile, prepareGenomeAssemblyWebinCli());
        assertThatManifestErrorIs(validator, "MANIFEST_ERROR_INVALID_FILE_GROUP");
    }

}
