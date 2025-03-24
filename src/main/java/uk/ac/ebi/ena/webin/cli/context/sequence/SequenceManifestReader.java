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
package uk.ac.ebi.ena.webin.cli.context.sequence;

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
import uk.ac.ebi.ena.webin.cli.validator.manifest.SequenceManifest;

public class SequenceManifestReader extends ManifestReader<SequenceManifest> {
  public interface Field {
    String STUDY = "STUDY";
    String RUN_REF = "RUN_REF";
    String ANALYSIS_REF = "ANALYSIS_REF";
    String DESCRIPTION = "DESCRIPTION";
    String TAB = "TAB";
    String FLATFILE = "FLATFILE";
    String FASTA = "FASTA";
    String ANALYSIS_TYPE = "ANALYSIS_TYPE";
    String AUTHORS = "AUTHORS";
    String ADDRESS = "ADDRESS";
  }

  public interface Description {
    String NAME = "Unique sequence submission name";
    String STUDY = "Study accession or name";
    String RUN_REF = "Run accession or name as a comma-separated list";
    String ANALYSIS_REF = "Analysis accession or name as a comma-separated list";
    String DESCRIPTION = "Sequence submission description";
    String TAB = "Tabulated file";
    String FLATFILE = "Flat file";
    String FASTA = "FASTA file";
    String ANALYSIS_TYPE =
        "Type of analysis (SEQUENCE_FLATFILE or SEQUENCE_SET). Default: SEQUENCE_FLATFILE";
    String AUTHORS = "For submission brokers only. Submitter's names as a comma-separated list";
    String ADDRESS = "For submission brokers only. Submitter's address";
  }

  public SequenceManifestReader(WebinCliParameters parameters, MetadataProcessorFactory factory) {
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
            .file()
            .optional()
            .name(Field.TAB)
            .desc(Description.TAB)
            .processor(getTabProcessors())
            .and()
            .file()
            .optional()
            .name(Field.FLATFILE)
            .desc(Description.FLATFILE)
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
            .group("Annotated sequences in a comma separated file.")
            .required(Field.TAB)
            .and()
            .group("Annotated sequences in a flat file.")
            .required(Field.FLATFILE)
            .and()
            .group("Both fasta and tab(tsv) files are mandatory for SEQUENCE_SET analysis.")
            .required(Field.FASTA)
            .required(Field.TAB)
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
              SequenceManifest manifest = getManifest(fieldGroup);

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

              manifest.setSubmissionTool(fieldGroup.getValue(Fields.SUBMISSION_TOOL));
              manifest.setSubmissionToolVersion(
                  fieldGroup.getValue(Fields.SUBMISSION_TOOL_VERSION));

              manifest.setIgnoreErrors(getWebinCliParameters().isIgnoreErrors());

              SubmissionFiles<SequenceManifest.FileType> submissionFiles = manifest.files();

              getFiles(getInputDir(), fieldGroup, Field.TAB)
                  .forEach(
                      tabFile ->
                          submissionFiles.add(
                              new SubmissionFile(SequenceManifest.FileType.TAB, tabFile)));
              getFiles(getInputDir(), fieldGroup, Field.FLATFILE)
                  .forEach(
                      flatFile ->
                          submissionFiles.add(
                              new SubmissionFile(SequenceManifest.FileType.FLATFILE, flatFile)));
              getFiles(getInputDir(), fieldGroup, Field.FASTA)
                  .forEach(
                      fastaFile ->
                          submissionFiles.add(
                              new SubmissionFile(SequenceManifest.FileType.FASTA, fastaFile)));
            });
  }

  @Override
  public Collection<SequenceManifest> getManifests() {
    return nameFieldToManifestMap.values();
  }

  @Override
  protected SequenceManifest createManifest() {
    return new SequenceManifest();
  }
}
