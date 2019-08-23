/*
 * Copyright 2018-2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.manifest.processor;

import uk.ac.ebi.ena.webin.cli.manifest.processor.metadata.*;

public class MetadataProcessorFactory {

  private MetadataProcessorParameters parameters;

  public MetadataProcessorFactory(MetadataProcessorParameters parameters) {
    this.parameters = parameters;
  }

  private SampleProcessor sampleProcessor;
  private StudyProcessor studyProcessor;
  private SampleXmlProcessor sampleXmlProcessor;
  private RunProcessor runProcessor;
  private AnalysisProcessor analysisProcessor;

  public SampleProcessor getSampleProcessor() {
    if (sampleProcessor != null) {
      return sampleProcessor;
    }
    if (parameters != null && parameters.isMetadataProcessorsActive()) {
      sampleProcessor = new SampleProcessor(parameters);
    }
    return sampleProcessor;
  }

  public StudyProcessor getStudyProcessor() {
    if (studyProcessor != null) {
      return studyProcessor;
    }
    if (parameters != null && parameters.isMetadataProcessorsActive()) {
      studyProcessor = new StudyProcessor(parameters);
    }
    return studyProcessor;
  }

  public SampleXmlProcessor getSampleXmlProcessor() {
    if (sampleXmlProcessor != null) {
      return sampleXmlProcessor;
    }
    if (parameters != null && parameters.isMetadataProcessorsActive()) {
      sampleXmlProcessor = new SampleXmlProcessor(parameters);
    }
    return sampleXmlProcessor;
  }

  public RunProcessor getRunProcessor() {
    if (runProcessor != null) {
      return runProcessor;
    }
    if (parameters != null && parameters.isMetadataProcessorsActive()) {
      runProcessor = new RunProcessor(parameters);
    }
    return runProcessor;
  }

  public AnalysisProcessor getAnalysisProcessor() {
    if (analysisProcessor != null) {
      return analysisProcessor;
    }
    if (parameters != null && parameters.isMetadataProcessorsActive()) {
      analysisProcessor = new AnalysisProcessor(parameters);
    }
    return analysisProcessor;
  }
}
