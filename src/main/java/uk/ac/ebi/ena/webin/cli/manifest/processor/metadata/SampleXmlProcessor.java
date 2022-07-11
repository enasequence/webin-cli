/*
 * Copyright 2018-2021 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.manifest.processor.metadata;

import org.springframework.web.client.HttpStatusCodeException;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorParameters;
import uk.ac.ebi.ena.webin.cli.service.SampleXmlService;
import uk.ac.ebi.ena.webin.cli.service.exception.ServiceException;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationMessage;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;
import uk.ac.ebi.ena.webin.cli.validator.reference.Sample;

public class SampleXmlProcessor implements ManifestFieldProcessor {
  private final MetadataProcessorParameters parameters;
  private ManifestFieldProcessor.Callback<Sample> callback;

  public SampleXmlProcessor(
      MetadataProcessorParameters parameters, ManifestFieldProcessor.Callback<Sample> callback) {
    this.parameters = parameters;
    this.callback = callback;
  }

  public SampleXmlProcessor(MetadataProcessorParameters parameters) {
    this.parameters = parameters;
  }

  public void setCallback(Callback<Sample> callback) {
    this.callback = callback;
  }

  @Override
  public void process(ValidationResult result, ManifestFieldValue fieldValue) {
    String value = fieldValue.getValue();

    try {
      SampleXmlService sampleXmlService =
          new SampleXmlService.Builder()
              .setCredentials(parameters.getWebinServiceUserName(), parameters.getPassword())
              .setTest(parameters.isTest())
              .build();

      Sample source = null;
      try {
        source = sampleXmlService.getSample(value);
      } catch (HttpStatusCodeException e) {
        handleHttpError(e, value);
      }

      callback.notify(source);

    } catch (WebinCliException e) {
      if (WebinCliMessage.CLI_AUTHENTICATION_ERROR.text().equals(e.getMessage())) {
        result.add(ValidationMessage.error(e));
      } else {
        result.add(
            ValidationMessage.error(
                WebinCliMessage.SAMPLE_PROCESSOR_LOOKUP_ERROR, value, e.getMessage()));
      }
    } catch (ServiceException e) {
      result.add(
          ValidationMessage.error(
              WebinCliMessage.SAMPLE_PROCESSOR_LOOKUP_ERROR, value, e.getMessage()));
    }
  }

  private void handleHttpError(HttpStatusCodeException ex, String sampleId) {
    switch (ex.getStatusCode()) {
      case UNAUTHORIZED:
      case FORBIDDEN:
        throw WebinCliException.userError(WebinCliMessage.CLI_AUTHENTICATION_ERROR.text());
      case NOT_FOUND:
        throw WebinCliException.validationError(WebinCliMessage.SAMPLE_SERVICE_VALIDATION_ERROR.format(sampleId));
      default:
        throw WebinCliException.systemError(WebinCliMessage.SAMPLE_SERVICE_SYSTEM_ERROR.format(sampleId));
    }
  }
}
