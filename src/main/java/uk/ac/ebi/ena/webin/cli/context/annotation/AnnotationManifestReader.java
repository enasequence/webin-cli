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
package uk.ac.ebi.ena.webin.cli.context.annotation;

import java.util.Collection;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestCVList;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldDefinition;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileCount;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileSuffix;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.ASCIIFileNameProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.CVFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.CustomFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.FileSuffixProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.manifest.AnnotationManifest;

public class AnnotationManifestReader extends ManifestReader<AnnotationManifest> {

  private static CustomFieldProcessor analysisAttributeProcessor;

  public static final String ANALYSIS_TYPE_DECOUPLED_ANNOTATION = "DECOUPLED_ANNOTATION";

  private static final ManifestCVList CV_ANALYSIS_TYPE =
      new ManifestCVList(ANALYSIS_TYPE_DECOUPLED_ANNOTATION);

  public interface Field {
    String GFF3 = "GFF3";
    String ANALYSIS_TYPE = "ANALYSIS_TYPE";
    String ANALYSIS_ATTRIBUTE = "ANALYSIS_ATTRIBUTE";
  }

  public interface Description {
    String NAME = "Unique annotation submission name";
    String GFF3 = "Annotation in a GFF3 file";
    String ANALYSIS_TYPE = "Analysis type";
    String ANALYSIS_ATTRIBUTE =
        "Additional annotation attribute in <tag>:<value> format. May be repeated.";
  }

  public AnnotationManifestReader(
      WebinCliParameters parameters, MetadataProcessorFactory factory) {
    super(
        parameters,
        // Fields.
        new ManifestFieldDefinition.Builder()
            .meta()
            .required()
            .name(Fields.NAME)
            .desc(Description.NAME)
            .and()
            .file()
            .required()
            .name(Field.GFF3)
            .desc(Description.GFF3)
            .processor(getGff3Processors())
            .and()
            .meta()
            .required()
            .name(Field.ANALYSIS_TYPE)
            .desc(Description.ANALYSIS_TYPE)
            .processor(new CVFieldProcessor(CV_ANALYSIS_TYPE))
            .and()
            .meta()
            .optional(100)
            .name(Field.ANALYSIS_ATTRIBUTE)
            .desc(Description.ANALYSIS_ATTRIBUTE)
            .processor(getAnalysisAttributeProcessor())
            .build(),
        // File groups.
        new ManifestFileCount.Builder()
            .group("Annotation in a GFF3 file.")
            .required(Field.GFF3)
            .build());

    getAnalysisAttributeProcessor()
        .setCallback(
            (fieldGroup, keyVal) ->
                getManifest(fieldGroup).addAttribute(keyVal.left, keyVal.right));
  }

  private static CustomFieldProcessor getAnalysisAttributeProcessor() {
    if (analysisAttributeProcessor == null) {
      analysisAttributeProcessor = new CustomFieldProcessor();
    }
    return analysisAttributeProcessor;
  }

  private static ManifestFieldProcessor[] getGff3Processors() {
    return new ManifestFieldProcessor[] {
      new ASCIIFileNameProcessor(), new FileSuffixProcessor(ManifestFileSuffix.GFF3_FILE_SUFFIX)
    };
  }

  @Override
  public void processManifest() {
    getManifestReaderResult()
        .getManifestFieldGroups()
        .forEach(
            fieldGroup -> {
              AnnotationManifest manifest = getManifest(fieldGroup);

              manifest.setName(fieldGroup.getValue(Fields.NAME));
              manifest.setAnalysisType(fieldGroup.getValue(Field.ANALYSIS_TYPE));
              manifest.setIgnoreErrors(getWebinCliParameters().isIgnoreErrors());

              SubmissionFiles<AnnotationManifest.FileType> submissionFiles = manifest.files();

              getFiles(getInputDir(), fieldGroup, Field.GFF3)
                  .forEach(
                      gff3File ->
                          submissionFiles.add(
                              new SubmissionFile(AnnotationManifest.FileType.GFF3, gff3File)));
            });
  }

  @Override
  public Collection<AnnotationManifest> getManifests() {
    return nameFieldToManifestMap.values();
  }

  @Override
  protected AnnotationManifest createManifest() {
    return new AnnotationManifest();
  }
}
