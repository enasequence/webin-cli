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
package uk.ac.ebi.ena.webin.cli.context.polysample;

import java.util.Collection;
import java.util.Map;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.manifest.*;
import uk.ac.ebi.ena.webin.cli.manifest.processor.ASCIIFileNameProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.AuthorProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.FileSuffixProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.manifest.PolySampleManifest;

public class PolySampleManifestReader extends ManifestReader<PolySampleManifest> {
  public interface Field {
    String STUDY = "STUDY";
    String RUN_REF = "RUN_REF";
    String ANALYSIS_REF = "ANALYSIS_REF";
    String DESCRIPTION = "DESCRIPTION";
    String SAMPLE_TSV = "SAMPLE_TSV";
    String TAX_TSV = "TAX_TSV";
    String FASTA = "FASTA";
    String ANALYSIS_TYPE = "ANALYSIS_TYPE";
    String ANALYSIS_PROTOCOL = "ANALYSIS_PROTOCOL";
    String ANALYSIS_DATE = "ANALYSIS DATE";
    String TARGET_LOCUS = "TARGET LOCUS";
    String ANALYSIS_CODE = "ANALYSIS CODE";
    String ANALYSIS_VERSION = "ANALYSIS VERSION";
    String ORGANELLE = "ORGANELLE";
    String FORWARD_PRIMER_NAME = "FORWARD PRIMER NAME";
    String FORWARD_PRIMER_SEQUENCE = "FORWARD PRIMER SEQUENCE";
    String REVERSE_PRIMER_NAME = "REVERSE PRIMER NAME";
    String REVERSE_PRIMER_SEQUENCE = "REVERSE PRIMER SEQUENCE";
    String ANALYSIS_CENTER = "ANALYSIS CENTER";
    String AUTHORS = "AUTHORS";
    String ADDRESS = "ADDRESS";
  }

  public interface Description {
    String NAME = "Unique sequence submission name";
    String STUDY = "Study accession or name";
    String RUN_REF = "Run accession or name as a comma-separated list";
    String ANALYSIS_REF = "Analysis accession or name as a comma-separated list";
    String DESCRIPTION = "Sequence submission description";
    String SAMPLE_TSV = "Tabulated file";
    String TAX_TSV = "Tabulated file";
    String FASTA = "FASTA file";
    String ANALYSIS_TYPE =
        "Type of SEQUENCE_SET, currently supported is ENVIRONMENTAL_SEQUENCE_SET";
    String ANALYSIS_PROTOCOL = "ANALYSIS PROTOCOL";
    String ANALYSIS_DATE = "ANALYSIS DATE";
    String TARGET_LOCUS = "TARGET LOCUS";
    String ANALYSIS_CODE = "ANALYSIS CODE";
    String ANALYSIS_VERSION = "ANALYSIS VERSION";
    String ORGANELLE = "ORGANELLE";
    String FORWARD_PRIMER_NAME = "FORWARD PRIMER NAME";
    String FORWARD_PRIMER_SEQUENCE = "FORWARD PRIMER SEQUENCE";
    String REVERSE_PRIMER_NAME = "REVERSE PRIMER NAME";
    String REVERSE_PRIMER_SEQUENCE = "REVERSE PRIMER SEQUENCE";
    String ANALYSIS_CENTER = "ANALYSIS CENTER";
    String AUTHORS = "For submission brokers only. Submitter's names as a comma-separated list";
    String ADDRESS = "For submission brokers only. Submitter's address";
  }

  public PolySampleManifestReader(WebinCliParameters parameters, MetadataProcessorFactory factory) {
    super(
        parameters,
        // Fields.
        new ManifestFieldDefinition.Builder()
            .meta()
            .required()
            .name(Fields.NAME)
            .desc(Description.NAME)
            .and()
            .meta()
            .required()
            .name(Field.STUDY)
            .desc(Description.STUDY)
            .processor(factory.getStudyProcessor())
            .and()
            .meta()
            .optional()
            .name(Field.RUN_REF)
            .desc(Description.RUN_REF)
            .processor(factory.getRunProcessor())
            .and()
            .meta()
            .optional()
            .name(Field.ANALYSIS_REF)
            .desc(Description.ANALYSIS_REF)
            .processor(factory.getAnalysisProcessor())
            .and()
            .meta()
            .optional()
            .name(Field.DESCRIPTION)
            .desc(Description.DESCRIPTION)
            .and()
            .meta()
            .optional()
            .name(Field.ANALYSIS_TYPE)
            .desc(Description.ANALYSIS_TYPE)
            .and()
            .meta()
            .optional()
            .name(Field.ANALYSIS_PROTOCOL)
            .desc(Description.ANALYSIS_PROTOCOL)
            .and()
            .meta()
            .optional()
            .name(Field.ANALYSIS_DATE)
            .desc(Description.ANALYSIS_DATE)
            .and()
            .meta()
            .optional()
            .name(Field.TARGET_LOCUS)
            .desc(Description.TARGET_LOCUS)
            .and()
            .meta()
            .optional()
            .name(Field.ANALYSIS_CODE)
            .desc(Description.ANALYSIS_CODE)
            .and()
            .meta()
            .optional()
            .name(Field.ANALYSIS_VERSION)
            .desc(Description.ANALYSIS_VERSION)
            .and()
            .meta()
            .optional()
            .name(Field.ORGANELLE)
            .desc(Description.ORGANELLE)
            .and()
            .meta()
            .optional()
            .name(Field.FORWARD_PRIMER_NAME)
            .desc(Description.FORWARD_PRIMER_NAME)
            .and()
            .meta()
            .optional()
            .name(Field.FORWARD_PRIMER_SEQUENCE)
            .desc(Description.FORWARD_PRIMER_SEQUENCE)
            .and()
            .meta()
            .optional()
            .name(Field.REVERSE_PRIMER_NAME)
            .desc(Description.REVERSE_PRIMER_NAME)
            .and()
            .meta()
            .optional()
            .name(Field.REVERSE_PRIMER_SEQUENCE)
            .desc(Description.REVERSE_PRIMER_SEQUENCE)
            .and()
            .meta()
            .optional()
            .name(Field.ANALYSIS_CENTER)
            .desc(Description.ANALYSIS_CENTER)
            .and()
            .file()
            .optional()
            .name(Field.SAMPLE_TSV)
            .desc(Description.SAMPLE_TSV)
            .processor(getTabProcessors())
            .and()
            .file()
            .optional()
            .name(Field.TAX_TSV)
            .desc(Description.TAX_TSV)
            .processor(getTabProcessors())
            .processor(getFlatfileProcessors())
            .and()
            .file()
            .optional()
            .name(Field.FASTA)
            .desc(Description.FASTA)
            .processor(getFastaProcessors())
            .and()
            .meta()
            .optional()
            .name(Field.AUTHORS)
            .desc(Description.AUTHORS)
            .processor(new AuthorProcessor())
            .and()
            .meta()
            .optional()
            .name(Field.ADDRESS)
            .desc(Description.ADDRESS)
            .and()
            .meta()
            .optional()
            .name(Fields.SUBMISSION_TOOL)
            .desc(Descriptions.SUBMISSION_TOOL)
            .and()
            .meta()
            .optional()
            .name(Fields.SUBMISSION_TOOL_VERSION)
            .desc(Descriptions.SUBMISSION_TOOL_VERSION)
            .build(),
        // File groups.
        new ManifestFileCount.Builder()
            .group(
                "A ENVIRONMENTAL_SEQUENCE_SET analysis submission with 1 FASTA "
                    + "+ 1 SAMPLE_TSV + 1 TAX_TSV")
            .required(Field.FASTA)
            .required(Field.SAMPLE_TSV)
            .required(Field.TAX_TSV)
            .and()
            .group(
                "A ENVIRONMENTAL_SEQUENCE_SET analysis submission with 1 FASTA " + "+ 1 SAMPLE_TSV")
            .required(Field.FASTA)
            .required(Field.SAMPLE_TSV)
            .and()
            .group("A ENVIRONMENTAL_SEQUENCE_SET analysis submission with 1 TAX_TSV")
            .required(Field.TAX_TSV)
            .build());

    if (factory.getStudyProcessor() != null) {
      factory
          .getStudyProcessor()
          .setCallback((fieldGroup, study) -> getManifest(fieldGroup).setStudy(study));
    }
    if (factory.getRunProcessor() != null) {
      factory
          .getRunProcessor()
          .setCallback((fieldGroup, run) -> getManifest(fieldGroup).setRun(run));
    }
    if (factory.getAnalysisProcessor() != null) {
      factory
          .getAnalysisProcessor()
          .setCallback((fieldGroup, analyses) -> getManifest(fieldGroup).setAnalysis(analyses));
    }
  }

  private static ManifestFieldProcessor[] getTabProcessors() {
    return new ManifestFieldProcessor[] {
      new ASCIIFileNameProcessor(), new FileSuffixProcessor(ManifestFileSuffix.TAB_FILE_SUFFIX)
    };
  }

  private static ManifestFieldProcessor[] getFlatfileProcessors() {
    return new ManifestFieldProcessor[] {
      new ASCIIFileNameProcessor(),
      new FileSuffixProcessor(ManifestFileSuffix.GZIP_OR_BZIP_FILE_SUFFIX)
    };
  }

  private static ManifestFieldProcessor[] getFastaProcessors() {
    return new ManifestFieldProcessor[] {
      new ASCIIFileNameProcessor(), new FileSuffixProcessor(ManifestFileSuffix.FASTA_FILE_SUFFIX)
    };
  }

  @Override
  public void processManifest() {
    getManifestReaderResult()
        .getManifestFieldGroups()
        .forEach(
            fieldGroup -> {
              PolySampleManifest manifest = getManifest(fieldGroup);

              if (getWebinCliParameters() != null) {
                manifest.setQuick(getWebinCliParameters().isQuick());
              }

              Map<String, String> authorAndAddress =
                  fieldGroup.getNonEmptyValues(Field.AUTHORS, Field.ADDRESS);
              if (!authorAndAddress.isEmpty()) {
                if (authorAndAddress.size() == 2) {
                  manifest.setAddress(authorAndAddress.get(Field.ADDRESS));
                  manifest.setAuthors(authorAndAddress.get(Field.AUTHORS));
                } else {
                  error(WebinCliMessage.MANIFEST_READER_MISSING_ADDRESS_OR_AUTHOR_ERROR);
                }
              }
              manifest.setName(fieldGroup.getValue(Fields.NAME));
              manifest.setDescription(fieldGroup.getValue(Field.DESCRIPTION));

              manifest.setAnalysisType(fieldGroup.getValue(Field.ANALYSIS_TYPE));
              manifest.setAnalysisProtocol(fieldGroup.getValue(Field.ANALYSIS_PROTOCOL));
              manifest.setAnalysisDate(fieldGroup.getValue(Field.ANALYSIS_DATE));
              manifest.setTargetLocus(fieldGroup.getValue(Field.TARGET_LOCUS));
              manifest.setAnalysisCode(fieldGroup.getValue(Field.ANALYSIS_CODE));
              manifest.setAnalysisVersion(fieldGroup.getValue(Field.ANALYSIS_VERSION));
              manifest.setOrganelle(fieldGroup.getValue(Field.ORGANELLE));
              manifest.setForwardPrimerName(fieldGroup.getValue(Field.FORWARD_PRIMER_NAME));
              manifest.setForwardPrimerSequence(fieldGroup.getValue(Field.FORWARD_PRIMER_SEQUENCE));
              manifest.setReversePrimerName(fieldGroup.getValue(Field.REVERSE_PRIMER_NAME));
              manifest.setReversePrimerSequence(fieldGroup.getValue(Field.REVERSE_PRIMER_SEQUENCE));
              manifest.setAnalysisCenter(fieldGroup.getValue(Field.ANALYSIS_CENTER));

              manifest.setSubmissionTool(fieldGroup.getValue(Fields.SUBMISSION_TOOL));
              manifest.setSubmissionToolVersion(
                  fieldGroup.getValue(Fields.SUBMISSION_TOOL_VERSION));

              manifest.setIgnoreErrors(getWebinCliParameters().isIgnoreErrors());

              SubmissionFiles<PolySampleManifest.FileType> submissionFiles = manifest.files();

              getFiles(getInputDir(), fieldGroup, Field.SAMPLE_TSV)
                  .forEach(
                      tabFile ->
                          submissionFiles.add(
                              new SubmissionFile(PolySampleManifest.FileType.SAMPLE_TSV, tabFile)));
              getFiles(getInputDir(), fieldGroup, Field.TAX_TSV)
                  .forEach(
                      tabFile ->
                          submissionFiles.add(
                              new SubmissionFile(PolySampleManifest.FileType.TAX_TSV, tabFile)));
              getFiles(getInputDir(), fieldGroup, Field.FASTA)
                  .forEach(
                      fastaFile ->
                          submissionFiles.add(
                              new SubmissionFile(PolySampleManifest.FileType.FASTA, fastaFile)));
            });
  }

  @Override
  public Collection<PolySampleManifest> getManifests() {
    return nameFieldToManifestMap.values();
  }

  @Override
  protected PolySampleManifest createManifest() {
    return new PolySampleManifest();
  }
}
