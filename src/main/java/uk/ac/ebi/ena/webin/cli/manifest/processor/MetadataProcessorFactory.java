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

import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.manifest.processor.metadata.*;
import uk.ac.ebi.ena.webin.cli.validator.reference.Sample;
import uk.ac.ebi.ena.webin.cli.validator.reference.Study;

public class MetadataProcessorFactory {

  private boolean active = true;

  public SampleProcessor createSampleProcessor(WebinCliParameters parameters ) {
    if (active) {
      return new SampleProcessor(parameters);
    }
    return null;
  }

  public StudyProcessor createStudyProcessor(WebinCliParameters parameters ) {
    if (active) {
      return new StudyProcessor(parameters);
    }
    return null;
  }

  public SampleXmlProcessor createSampleXmlProcessor(WebinCliParameters parameters ) {
    if (active) {
      return new SampleXmlProcessor(parameters);
    }
    return null;
  }
  
  public RunProcessor createRunProcessor(WebinCliParameters parameters ) {
    if (active) {
      return new RunProcessor(parameters);
    }
    return null;
  }
  
  public AnalysisProcessor createAnalysisProcessor(WebinCliParameters parameters ) {
    if (active) {
      return new AnalysisProcessor(parameters);
    }
    return null;
  }

  public SampleProcessor createSampleProcessor(WebinCliParameters parameters, SampleProcessor.Callback<Sample> callback) {
    if (active) {
      return new SampleProcessor(parameters, callback);
    }
    return null;
  }

  public StudyProcessor createStudyProcessor(WebinCliParameters parameters, StudyProcessor.Callback<Study> callback ) {
    if (active) {
      return new StudyProcessor(parameters, callback);
    }
    return null;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
