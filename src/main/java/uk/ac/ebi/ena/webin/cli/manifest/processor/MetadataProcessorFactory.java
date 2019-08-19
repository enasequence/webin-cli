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

  public SampleProcessor createSampleProcessor() {
    if (parameters != null && parameters.isMetadataProcessorsActive()) {
      return new SampleProcessor(parameters);
    }
    return null;
  }

  public StudyProcessor createStudyProcessor() {
    if (parameters != null && parameters.isMetadataProcessorsActive()) {
      return new StudyProcessor(parameters);
    }
    return null;
  }

  public SampleXmlProcessor createSampleXmlProcessor() {
    if (parameters != null && parameters.isMetadataProcessorsActive()) {
      return new SampleXmlProcessor(parameters);
    }
    return null;
  }

  public RunProcessor createRunProcessor() {
    if (parameters != null && parameters.isMetadataProcessorsActive()) {
      return new RunProcessor(parameters);
    }
    return null;
  }

  public AnalysisProcessor createAnalysisProcessor() {
    if (parameters != null && parameters.isMetadataProcessorsActive()) {
      return new AnalysisProcessor(parameters);
    }
    return null;
  }
}
