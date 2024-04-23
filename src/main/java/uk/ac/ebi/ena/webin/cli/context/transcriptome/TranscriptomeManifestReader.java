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
package uk.ac.ebi.ena.webin.cli.context.transcriptome;

import java.util.Collection;
import java.util.Map;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestCVList;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldDefinition;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileCount;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileSuffix;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.ASCIIFileNameProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.AuthorProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.CVFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.FileSuffixProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TranscriptomeManifest;

public class TranscriptomeManifestReader extends ManifestReader<TranscriptomeManifest> {
  public interface Field {
    String ASSEMBLYNAME = "ASSEMBLYNAME";
    String STUDY = "STUDY";
    String SAMPLE = "SAMPLE";
    String RUN_REF = "RUN_REF";
    String ANALYSIS_REF = "ANALYSIS_REF";
    String DESCRIPTION = "DESCRIPTION";
    String PROGRAM = "PROGRAM";
    String PLATFORM = "PLATFORM";
    String TPA = "TPA";
    String FASTA = "FASTA";
    String FLATFILE = "FLATFILE";
    String AUTHORS = "AUTHORS";
    String ADDRESS = "ADDRESS";
    String ASSEMBLY_TYPE = "ASSEMBLY_TYPE";
  }

  public interface Description {
    String NAME = "Unique transcriptome assembly name";
    String STUDY = "Study accession or name";
    String SAMPLE = "Sample accession or name";
    String RUN_REF = "Run accession or name as a comma-separated list";
    String ANALYSIS_REF = "Analysis accession or name as a comma-separated list";

    String DESCRIPTION = "Transcriptome assembly description";
    String PROGRAM = "Assembly program";
    String PLATFORM = "Sequencing platform";
    String TPA = "Third party annotation";
    String FASTA = "Fasta file";
    String FLATFILE = "Flat file";
    String AUTHORS = "For submission brokers only. Submitter's names as a comma-separated list";
    String ADDRESS = "For submission brokers only. Submitter's address";
    String ASSEMBLY_TYPE = "Assembly type";
  }

  private static final ManifestCVList CV_ASSEMBLY_TYPE =
      new ManifestCVList("isolate", "metatranscriptome");

  public TranscriptomeManifestReader(
      WebinCliParameters parameters, MetadataProcessorFactory factory) {
    super(
        parameters,
        // Fields.
        new ManifestFieldDefinition.Builder()
            .meta()
            .required()
            .name(Fields.NAME)
            .desc(Description.NAME)
            .synonym(Field.ASSEMBLYNAME)
            .and()
            .meta()
            .required()
            .name(Field.STUDY)
            .desc(Description.STUDY)
            .processor(factory.getStudyProcessor())
            .and()
            .meta()
            .required()
            .name(Field.SAMPLE)
            .desc(Description.SAMPLE)
            .processor(factory.getSampleProcessor(), factory.getSampleXmlProcessor())
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
            .required()
            .name(Field.PROGRAM)
            .desc(Description.PROGRAM)
            .and()
            .meta()
            .required()
            .name(Field.PLATFORM)
            .desc(Description.PLATFORM)
            .and()
            .file()
            .optional()
            .name(Field.FASTA)
            .desc(Description.FASTA)
            .processor(getFastaProcessors())
            .and()
            .file()
            .optional()
            .name(Field.FLATFILE)
            .desc(Description.FLATFILE)
            .processor(getFlatfileProcessors())
            .and()
            .meta()
            .optional()
            .name(Field.TPA)
            .desc(Description.TPA)
            .processor(CVFieldProcessor.CV_BOOLEAN)
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
            .required()
            .name(Field.ASSEMBLY_TYPE)
            .desc(Description.ASSEMBLY_TYPE)
            .processor(new CVFieldProcessor(CV_ASSEMBLY_TYPE))
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
            .group("Sequences in a fasta file.")
            .required(Field.FASTA)
            .and()
            .group("Sequences in an annotated flat file.")
            .required(Field.FLATFILE)
            .build());

    if (factory.getStudyProcessor() != null) {
      factory.getStudyProcessor().setCallback(
          (fieldGroup, study) -> getManifest(fieldGroup).setStudy(study));
    }
    if (factory.getSampleProcessor() != null) {
      factory.getSampleProcessor().setCallback(
          (fieldGroup, sample) -> getManifest(fieldGroup).setSample(sample));
    }
    if (factory.getRunProcessor() != null) {
      factory.getRunProcessor().setCallback(
          (fieldGroup, runs) -> getManifest(fieldGroup).setRun(runs));
    }
    if (factory.getAnalysisProcessor() != null) {
      factory.getAnalysisProcessor().setCallback(
          (fieldGroup, analyses) -> getManifest(fieldGroup).setAnalysis(analyses));
    }
    if (factory.getSampleXmlProcessor() != null) {
      factory.getSampleXmlProcessor().setCallback((fieldGroup, sample) -> {
        TranscriptomeManifest manifest = getManifest(fieldGroup);

        manifest.getSample().setName(sample.getName());
        manifest.getSample().setAttributes(sample.getAttributes());
      });
    }
  }

  private static ManifestFieldProcessor[] getFastaProcessors() {
    return new ManifestFieldProcessor[] {
      new ASCIIFileNameProcessor(), new FileSuffixProcessor(ManifestFileSuffix.FASTA_FILE_SUFFIX)
    };
  }

  private static ManifestFieldProcessor[] getFlatfileProcessors() {
    return new ManifestFieldProcessor[] {
      new ASCIIFileNameProcessor(),
      new FileSuffixProcessor(ManifestFileSuffix.GZIP_OR_BZIP_FILE_SUFFIX)
    };
  }

  @Override
  public void processManifest() {
    getManifestReaderResult().getManifestFieldGroups().forEach(fieldGroup -> {
      TranscriptomeManifest manifest = getManifest(fieldGroup);

      if (getWebinCliParameters() != null) {
        manifest.setQuick(getWebinCliParameters().isQuick());
      }

      manifest.setName(fieldGroup.getValue(Fields.NAME));
      manifest.setDescription(fieldGroup.getValue(Field.DESCRIPTION));

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

      manifest.setProgram(fieldGroup.getValue(Field.PROGRAM));
      manifest.setPlatform(fieldGroup.getValue(Field.PLATFORM));
      manifest.setAssemblyType(fieldGroup.getValue(Field.ASSEMBLY_TYPE));

      if (fieldGroup.getCount(Field.TPA) > 0) {
        manifest.setTpa(getAndValidateBoolean(fieldGroup.getField(Field.TPA)));
      }

      manifest.setSubmissionTool(fieldGroup.getValue(Fields.SUBMISSION_TOOL));
      manifest.setSubmissionToolVersion(
          fieldGroup.getValue(Fields.SUBMISSION_TOOL_VERSION));

      manifest.setIgnoreErrors(getWebinCliParameters().isIgnoreErrors());

      SubmissionFiles<TranscriptomeManifest.FileType> submissionFiles = manifest.files();

      getFiles(getInputDir(), fieldGroup, Field.FASTA)
          .forEach(
              file ->
                  submissionFiles.add(
                      new SubmissionFile(TranscriptomeManifest.FileType.FASTA, file)));
      getFiles(getInputDir(), fieldGroup, Field.FLATFILE)
          .forEach(
              file ->
                  submissionFiles.add(
                      new SubmissionFile(TranscriptomeManifest.FileType.FLATFILE, file)));
    });
  }

  @Override
  public Collection<TranscriptomeManifest> getManifests() {
    return nameFieldToManifestMap.values();
  }

  @Override
  protected TranscriptomeManifest createManifest() {
    return new TranscriptomeManifest();
  }
}
