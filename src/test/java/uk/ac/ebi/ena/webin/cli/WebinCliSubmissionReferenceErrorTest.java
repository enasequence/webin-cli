/*
 * Copyright 2018-2023 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli;

import static uk.ac.ebi.ena.webin.cli.WebinCliSubmissionTest.testGenomeError;
import static uk.ac.ebi.ena.webin.cli.WebinCliSubmissionTest.testSequenceError;

import org.junit.Test;

public class WebinCliSubmissionReferenceErrorTest {

  @Test
  public void testGenomeUnknownSampleError() {
    String sample = "INVALID";
    testGenomeError(
        m ->
            m.field("NAME", WebinCliTestUtils.generateUniqueManifestName())
                .field("ASSEMBLY_TYPE", "COVID-19 outbreak")
                .field("STUDY", "ERP011959")
                .field("SAMPLE", sample)
                .field("COVERAGE", "1.0")
                .field("PROGRAM", "prog-123")
                .field("PLATFORM", "ILLUMINA"),
        m ->
            m.file("FASTA", "valid-covid19.fasta.gz")
                .file("CHROMOSOME_LIST", "valid-covid19-chromosome.list.gz"),
        r ->
            r.textInManifestReport(
                "Unknown sample "
                    + sample
                    + " or the sample cannot be referenced by your submission account."),
        WebinCliException.ErrorType.USER_ERROR);
  }

  @Test
  public void testGenomeUnknownStudyError() {
    String study = "INVALID";
    testGenomeError(
        m ->
            m.field("NAME", WebinCliTestUtils.generateUniqueManifestName())
                .field("ASSEMBLY_TYPE", "COVID-19 outbreak")
                .field("STUDY", study)
                .field("SAMPLE", "ERS829308")
                .field("COVERAGE", "1.0")
                .field("PROGRAM", "prog-123")
                .field("PLATFORM", "ILLUMINA"),
        m ->
            m.file("FASTA", "valid-covid19.fasta.gz")
                .file("CHROMOSOME_LIST", "valid-covid19-chromosome.list.gz"),
        r ->
            r.textInManifestReport(
                "Unknown study "
                    + study
                    + " or the study cannot be referenced by your submission account."),
        WebinCliException.ErrorType.USER_ERROR);
  }

  @Test
  public void testSequenceUnknownRunError() {
    String run = "INVALID";
    testSequenceError(
        m ->
            m.field("NAME", WebinCliTestUtils.generateUniqueManifestName())
                .field("STUDY", "PRJEB20083")
                .field("RUN_REF", run)
                .field("ANALYSIS_REF", "ERZ690501, ERZ690500"),
        m ->
            m.field("DESCRIPTION", "Some sequence assembly description")
                .file("TAB", "valid/ERT000003-EST.tsv.gz"),
        r ->
            r.textInManifestReport(
                "Unknown run "
                    + run
                    + " or the run cannot be referenced by your submission account. Runs must be submitted before they can be referenced in the submission."),
        WebinCliException.ErrorType.USER_ERROR);
  }

  @Test
  public void testSequenceUnknownAnalysisError() {
    String analysis = "INVALID";
    testSequenceError(
        m ->
            m.field("NAME", WebinCliTestUtils.generateUniqueManifestName())
                .field("STUDY", "PRJEB20083")
                .field("RUN_REF", "ERR2836762, ERR2836753, SRR8083599")
                .field("ANALYSIS_REF", analysis),
        m ->
            m.field("DESCRIPTION", "Some sequence assembly description")
                .file("TAB", "valid/ERT000003-EST.tsv.gz"),
        r ->
            r.textInManifestReport(
                "Unknown analysis "
                    + analysis
                    + " or the analysis cannot be referenced by your submission account."),
        WebinCliException.ErrorType.USER_ERROR);
  }
}
