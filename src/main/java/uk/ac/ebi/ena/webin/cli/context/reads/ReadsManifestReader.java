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
package uk.ac.ebi.ena.webin.cli.context.reads;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestCVList;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldDefinition;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileCount;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileSuffix;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.ASCIIFileNameProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.CVFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.FileSuffixProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFileAttribute;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest.QualityScore;

public class ReadsManifestReader extends ManifestReader<ReadsManifest> {

  public interface Field {
    String NAME = "NAME";
    String STUDY = "STUDY";
    String SAMPLE = "SAMPLE";
    String PLATFORM = "PLATFORM";
    String INSTRUMENT = "INSTRUMENT";
    String DESCRIPTION = "DESCRIPTION";
    String LIBRARY_SOURCE = "LIBRARY_SOURCE";
    String LIBRARY_SELECTION = "LIBRARY_SELECTION";
    String LIBRARY_STRATEGY = "LIBRARY_STRATEGY";
    String LIBRARY_CONSTRUCTION_PROTOCOL = "LIBRARY_CONSTRUCTION_PROTOCOL";
    String LIBRARY_NAME = "LIBRARY_NAME";
    String INSERT_SIZE = "INSERT_SIZE";
    String QUALITY_SCORE = "QUALITY_SCORE";
    String __HORIZON = "__HORIZON";
    String FASTQ = "FASTQ";
    String BAM = "BAM";
    String CRAM = "CRAM";
    String READ_TYPE = "READ_TYPE";
  }

  public interface Description {
    String NAME = "Unique sequencing experiment name";
    String STUDY = "Study accession or name";
    String SAMPLE = "Sample accession or name";
    String PLATFORM = "Sequencing platform";
    String INSTRUMENT = "Sequencing instrument";
    String DESCRIPTION = "Experiment description";
    String LIBRARY_SOURCE = "Source material";
    String LIBRARY_SELECTION = "Method used to select or enrich the source material";
    String LIBRARY_STRATEGY = "Sequencing technique";
    String LIBRARY_CONSTRUCTION_PROTOCOL = "Protocol used to construct the sequencing library";
    String LIBRARY_NAME = "Library name";
    String INSERT_SIZE = "Insert size for paired reads";
    String QUALITY_SCORE = "";
    String __HORIZON = "";
    String FASTQ = "Fastq file";
    String BAM = "BAM file";
    String CRAM = "CRAM file";
    String READ_TYPE = "10x Fastq read types";
  }

  private static final String INSTRUMENT_UNSPECIFIED = "unspecified";
  public static final String QUALITY_SCORE_PHRED_33 = "PHRED_33";
  public static final String QUALITY_SCORE_PHRED_64 = "PHRED_64";
  public static final String QUALITY_SCORE_LOGODDS = "LOGODDS";

  public static final ManifestCVList CV_INSTRUMENT =
      new ManifestCVList(new File("uk/ac/ebi/ena/webin/cli/reads/instrument.properties"));
  public static final ManifestCVList CV_PLATFORM =
      new ManifestCVList(new File("uk/ac/ebi/ena/webin/cli/reads/platform.properties"));
  public static final ManifestCVList CV_SELECTION =
      new ManifestCVList(new File("uk/ac/ebi/ena/webin/cli/reads/selection.properties"));
  public static final ManifestCVList CV_SOURCE =
      new ManifestCVList(new File("uk/ac/ebi/ena/webin/cli/reads/source.properties"));
  public static final ManifestCVList CV_STRATEGY =
      new ManifestCVList(new File("uk/ac/ebi/ena/webin/cli/reads/strategy.properties"));
  public static final ManifestCVList CV_QUALITY_SCORE =
      new ManifestCVList(QUALITY_SCORE_PHRED_33, QUALITY_SCORE_PHRED_64, QUALITY_SCORE_LOGODDS);
  public static final ManifestCVList CV_READ_TYPE =
      new ManifestCVList(
          "single",
          "paired",
          "cell_barcode",
          "umi_barcode",
          "feature_barcode",
          "sample_barcode",
          "spatial_barcode");

  /** Meta fields are optional and ignored during validation if -validateFiles option is given. */
  private static boolean isValidateFiles(WebinCliParameters parameters) {
    return parameters != null && parameters.isValidateFiles();
  }

  public ReadsManifestReader(WebinCliParameters parameters, MetadataProcessorFactory factory) {
    super(
        parameters,
        // Fields.
        new ManifestFieldDefinition.Builder()
            .meta()
            .optionalIf(isValidateFiles(parameters))
            .name(Field.NAME)
            .desc(Description.NAME)
            .and()
            .meta()
            .optionalIf(isValidateFiles(parameters))
            .name(Field.STUDY)
            .desc(Description.STUDY)
            .processor(factory.getStudyProcessor())
            .and()
            .meta()
            .optionalIf(isValidateFiles(parameters))
            .name(Field.SAMPLE)
            .desc(Description.SAMPLE)
            .processor(factory.getSampleProcessor())
            .and()
            .meta()
            .optional()
            .name(Field.DESCRIPTION)
            .desc(Description.DESCRIPTION)
            .and()
            .meta()
            .optional()
            .recommended()
            .name(Field.INSTRUMENT)
            .desc(Description.INSTRUMENT)
            .processor(new CVFieldProcessor(CV_INSTRUMENT))
            .and()
            .meta()
            .optional()
            .name(Field.PLATFORM)
            .desc(Description.PLATFORM)
            .processor(new CVFieldProcessor(CV_PLATFORM))
            .and()
            .meta()
            .optionalIf(isValidateFiles(parameters))
            .name(Field.LIBRARY_SOURCE)
            .desc(Description.LIBRARY_SOURCE)
            .processor(new CVFieldProcessor(CV_SOURCE))
            .and()
            .meta()
            .optionalIf(isValidateFiles(parameters))
            .name(Field.LIBRARY_SELECTION)
            .desc(Description.LIBRARY_SELECTION)
            .processor(new CVFieldProcessor(CV_SELECTION))
            .and()
            .meta()
            .optionalIf(isValidateFiles(parameters))
            .name(Field.LIBRARY_STRATEGY)
            .desc(Description.LIBRARY_STRATEGY)
            .processor(new CVFieldProcessor(CV_STRATEGY))
            .and()
            .meta()
            .optional()
            .name(Field.LIBRARY_CONSTRUCTION_PROTOCOL)
            .desc(Description.LIBRARY_CONSTRUCTION_PROTOCOL)
            .and()
            .meta()
            .optional()
            .name(Field.LIBRARY_NAME)
            .desc(Description.LIBRARY_NAME)
            .and()
            .meta()
            .optional()
            .name(Field.INSERT_SIZE)
            .desc(Description.INSERT_SIZE)
            .and()
            .file()
            .optional(10)
            .name(Field.FASTQ)
            .desc(Description.FASTQ)
            .processor(getFastqProcessors())
            .attributes(
                new ManifestFieldDefinition.Builder()
                    .attribute()
                    .optional()
                    .name(Field.READ_TYPE)
                    .desc(Description.READ_TYPE)
                    .processor(new CVFieldProcessor(CV_READ_TYPE))
                    .build())
            .and()
            .file()
            .optional()
            .name(Field.BAM)
            .desc(Description.BAM)
            .processor(getBamProcessors())
            .and()
            .file()
            .optional()
            .name(Field.CRAM)
            .desc(Description.CRAM)
            .processor(getCramProcessors())
            .and()
            .meta()
            .optional()
            .hidden()
            .name(Field.QUALITY_SCORE)
            .desc(Description.QUALITY_SCORE)
            .processor(new CVFieldProcessor(CV_QUALITY_SCORE))
            .and()
            .meta()
            .optional()
            .hidden()
            .name(Field.__HORIZON)
            .desc(Description.__HORIZON)
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
            .group("Single or paired sequence reads in multiple fastq files.")
            .required(Field.FASTQ, 10)
            .and()
            .group("Sequence reads in a CRAM file.")
            .required(Field.CRAM)
            .and()
            .group("Sequence reads in a BAM file.")
            .required(Field.BAM)
            .build());

    if (factory.getStudyProcessor() != null) {
      factory.getStudyProcessor().setCallback(
          (fieldGroup, study) -> getManifest(fieldGroup).setStudy(study));
    }
    if (factory.getSampleProcessor() != null) {
      factory.getSampleProcessor().setCallback(
          (fieldGroup, sample) -> getManifest(fieldGroup).setSample(sample));
    }
  }

  private static ManifestFieldProcessor[] getFastqProcessors() {
    return new ManifestFieldProcessor[] {
      new ASCIIFileNameProcessor(),
      new FileSuffixProcessor(ManifestFileSuffix.GZIP_OR_BZIP_FILE_SUFFIX)
    };
  }

  private static ManifestFieldProcessor[] getBamProcessors() {
    return new ManifestFieldProcessor[] {
      new ASCIIFileNameProcessor(), new FileSuffixProcessor(ManifestFileSuffix.BAM_FILE_SUFFIX)
    };
  }

  private static ManifestFieldProcessor[] getCramProcessors() {
    return new ManifestFieldProcessor[] {
      new ASCIIFileNameProcessor(), new FileSuffixProcessor(ManifestFileSuffix.CRAM_FILE_SUFFIX)
    };
  }

  @Override
  public void processManifest() {
    getManifestReaderResult().getManifestFieldGroups().forEach(fieldGroup -> {
      ReadsManifest manifest = getManifest(fieldGroup);

      if (getWebinCliParameters() != null) {
        manifest.setQuick(getWebinCliParameters().isQuick());
      }

      manifest.setName(fieldGroup.getValue(Field.NAME));
      manifest.setDescription(fieldGroup.getValue(Field.DESCRIPTION));

      if (fieldGroup.getCount(Field.INSTRUMENT) > 0
          && fieldGroup.getField(Field.INSTRUMENT).isValidFieldValueOrFileSuffix())
        manifest.setInstrument(fieldGroup.getValue(Field.INSTRUMENT));

      if (fieldGroup.getCount(Field.PLATFORM) > 0
          && fieldGroup.getField(Field.PLATFORM).isValidFieldValueOrFileSuffix())
        manifest.setPlatform(fieldGroup.getValue(Field.PLATFORM));

      manifest.setInsertSize(
          getAndValidatePositiveInteger(fieldGroup.getField(Field.INSERT_SIZE)));

      if (fieldGroup.getCount(Field.LIBRARY_SOURCE) > 0
          && fieldGroup.getField(Field.LIBRARY_SOURCE).isValidFieldValueOrFileSuffix())
        manifest.setLibrarySource(fieldGroup.getValue(Field.LIBRARY_SOURCE));

      if (fieldGroup.getCount(Field.LIBRARY_SELECTION) > 0
          && fieldGroup
          .getField(Field.LIBRARY_SELECTION)
          .isValidFieldValueOrFileSuffix())
        manifest.setLibrarySelection(fieldGroup.getValue(Field.LIBRARY_SELECTION));

      if (fieldGroup.getCount(Field.LIBRARY_STRATEGY) > 0
          && fieldGroup
          .getField(Field.LIBRARY_STRATEGY)
          .isValidFieldValueOrFileSuffix())
        manifest.setLibraryStrategy(fieldGroup.getValue(Field.LIBRARY_STRATEGY));

      manifest.setLibraryConstructionProtocol(
          fieldGroup.getValue(Field.LIBRARY_CONSTRUCTION_PROTOCOL));
      manifest.setLibraryName(fieldGroup.getValue(Field.LIBRARY_NAME));

      if (fieldGroup.getCount(Field.QUALITY_SCORE) > 0) {
        String qsStr = fieldGroup.getValue(Field.QUALITY_SCORE);
        try {
          QualityScore qs = QualityScore.valueOf(qsStr);
          manifest.setQualityScore(qs);
        } catch (Exception ex) {
          error(
              WebinCliMessage.READS_MANIFEST_READER_INVALID_QUALITY_SCORE_ERROR,
              qsStr);
        }
      }

      if (fieldGroup.getCount(Field.__HORIZON) > 0)
        manifest.setPairingHorizon(
            getAndValidatePositiveInteger(fieldGroup.getField(Field.__HORIZON)));

      manifest.setSubmissionTool(fieldGroup.getValue(Fields.SUBMISSION_TOOL));
      manifest.setSubmissionToolVersion(
          fieldGroup.getValue(Fields.SUBMISSION_TOOL_VERSION));

      manifest.setIgnoreErrors(getWebinCliParameters().isIgnoreErrors());

      processInstrumentAndPlatform(manifest);

      SubmissionFiles<ReadsManifest.FileType> submissionFiles = manifest.files();

      getFiles(getInputDir(), fieldGroup, ReadsManifestReader.Field.BAM)
          .forEach(file -> submissionFiles.add(new SubmissionFile(ReadsManifest.FileType.BAM, file)));
      getFiles(getInputDir(), fieldGroup, ReadsManifestReader.Field.CRAM)
          .forEach(file -> submissionFiles.add(new SubmissionFile(ReadsManifest.FileType.CRAM, file)));

      fieldGroup.stream()
          .filter(field -> field.getDefinition().getType() == ManifestFieldType.FILE
              && field.getName().equals(ReadsManifestReader.Field.FASTQ))
          .forEach(field -> {
                File file = getFile(getInputDir(), field).toPath().normalize().toFile();

                List<SubmissionFileAttribute> fileAttributes = getAttributes(field).stream()
                    .map(entry -> new SubmissionFileAttribute(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

                submissionFiles.add(new SubmissionFile(ReadsManifest.FileType.FASTQ, file, fileAttributes));
              });
    });
  }

  private void processInstrumentAndPlatform(ReadsManifest manifest) {
    if (null == manifest.getPlatform()
        && (null == manifest.getInstrument()
            || manifest.getInstrument().equals(INSTRUMENT_UNSPECIFIED))) {
      error(
          WebinCliMessage.READS_MANIFEST_READER_MISSING_PLATFORM_AND_INSTRUMENT_ERROR,
          String.join(", ", CV_PLATFORM.keyList()),
          String.join(", ", CV_INSTRUMENT.keyList()));
    }

    if (manifest.getInstrument() != null) {
      // Set platform.

      String platforms = CV_INSTRUMENT.getValue(manifest.getInstrument());
      if (StringUtils.isBlank(platforms)) {
        error(
            WebinCliMessage.READS_MANIFEST_READER_MISSING_PLATFORM_FOR_INSTRUMENT_ERROR,
            manifest.getInstrument());
      }

      String[] platformList = platforms.split("[;,]");

      if (1 == platformList.length) {
        manifest.setPlatform(CV_PLATFORM.getKey(platformList[0]));
      } else if (Stream.of(platformList).noneMatch(e -> e.equals(manifest.getPlatform()))) {
        error(
            WebinCliMessage.READS_MANIFEST_READER_INVALID_PLATFORM_FOR_INSTRUMENT_ERROR,
            StringUtils.isBlank(manifest.getPlatform())
                ? "is not defined"
                : manifest.getPlatform() + " is not supported",
            manifest.getInstrument(),
            CV_INSTRUMENT.getValue(manifest.getInstrument()));
      }
    } else {
      manifest.setInstrument(INSTRUMENT_UNSPECIFIED);
    }
  }

  @Override
  public Collection<ReadsManifest> getManifests() {
    return nameFieldToManifestMap.values();
  }

  @Override
  protected ReadsManifest createManifest() {
    return new ReadsManifest();
  }
}
