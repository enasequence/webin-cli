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
  private CustomFieldProcessor customFieldProcessor;

  public SampleProcessor getSampleProcessor() {
    if (parameters != null && parameters.getSampleProcessor() != null) {
      return parameters.getSampleProcessor();
    }
    if (parameters != null && sampleProcessor == null) {
      sampleProcessor = new SampleProcessor(parameters);
    }
    return sampleProcessor;
  }

  public StudyProcessor getStudyProcessor() {
    if (parameters != null && parameters.getStudyProcessor() != null) {
      return parameters.getStudyProcessor();
    }
    if (parameters != null && studyProcessor == null) {
      studyProcessor = new StudyProcessor(parameters);
    }
    return studyProcessor;
  }
  
  public SampleXmlProcessor getSampleXmlProcessor() {
    if (parameters != null && parameters.getSampleXmlProcessor() != null) {
      return parameters.getSampleXmlProcessor();
    }
    if (parameters != null && sampleXmlProcessor == null) {
      sampleXmlProcessor = new SampleXmlProcessor(parameters);
    }
    return sampleXmlProcessor;
  }

  public RunProcessor getRunProcessor() {
    if (parameters != null && parameters.getRunProcessor() != null) {
      return parameters.getRunProcessor();
    }
    if (parameters != null && runProcessor == null) {
      runProcessor = new RunProcessor(parameters);
    }
    return runProcessor;
  }

  public AnalysisProcessor getAnalysisProcessor() {
    if (parameters != null && parameters.getAnalysisProcessor() != null) {
      return parameters.getAnalysisProcessor();
    }
    if (parameters != null && analysisProcessor == null) {
      analysisProcessor = new AnalysisProcessor(parameters);
    }
    return analysisProcessor;
  }

  public CustomFieldProcessor getCustomFieldProcessor() {

    if ( customFieldProcessor == null) {
      customFieldProcessor = new CustomFieldProcessor();
    }
    return customFieldProcessor;
  }
}
