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
package uk.ac.ebi.ena.webin.cli.manifest.processor.metadata;


import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorParameters;
import uk.ac.ebi.ena.webin.cli.service.SampleService;
import uk.ac.ebi.ena.webin.cli.service.SubmitService;
import uk.ac.ebi.ena.webin.cli.utils.ExceptionUtils;
import uk.ac.ebi.ena.webin.cli.utils.RemoteServiceUrlHelper;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationMessage;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;
import uk.ac.ebi.ena.webin.cli.validator.reference.Sample;

public class SampleProcessor implements ManifestFieldProcessor {

  private final MetadataProcessorParameters parameters;
  private ManifestFieldProcessor.Callback<Sample> callback;

  public SampleProcessor(
      MetadataProcessorParameters parameters, ManifestFieldProcessor.Callback<Sample> callback) {
    this.parameters = parameters;
    this.callback = callback;
  }

  public SampleProcessor(MetadataProcessorParameters parameters) {
    this.parameters = parameters;
  }

  public void setCallback(Callback<Sample> callback) {
    this.callback = callback;
  }

  public Callback<Sample> getCallback() {
    return callback;
  }

  @Override
  public void process(ValidationResult result, ManifestFieldValue fieldValue) {
    String sampleValue = fieldValue.getValue();
    try {
      if (isJsonSample(sampleValue)) {
        JsonNode sampleJson = new ObjectMapper().readTree(sampleValue);
        String action = isSampleExists(getAlias(sampleJson)) ? "MODIFY" : "ADD";
        String sampleSubmission = generateSampleSubmissionJson(action, sampleJson);
        if (action.equals("ADD") || (action.equals("MODIFY") && parameters.isSampleUpdate())) {
          getSubmitService().doJsonSubmission(sampleSubmission);
        }
        sampleValue = getAlias(sampleJson);
      }
      
      Sample sample = getSample(sampleValue);
      fieldValue.setValue(sample.getBioSampleId());
      callback.notify(sample);

    } catch (WebinCliException | JsonProcessingException e) {
      result.add(ValidationMessage.error(e));
    }
  }
  
  private Sample getSample(String sampleIdOrAlias){
  
    return  ExceptionUtils.executeWithRestExceptionHandling(() -> getSampleService().getSample(sampleIdOrAlias),
            WebinCliMessage.SERVICE_AUTHENTICATION_ERROR.format(SampleService.SERVICE_NAME),
            WebinCliMessage.SAMPLE_SERVICE_VALIDATION_ERROR.format(sampleIdOrAlias),
            WebinCliMessage.SAMPLE_SERVICE_SYSTEM_ERROR.format(sampleIdOrAlias));
  
  }

  private boolean isSampleExists(String sampleIdOrAlias){
    try {
      return getSampleService().getSample(sampleIdOrAlias)!=null;
    }catch (HttpClientErrorException.NotFound notFound){
      return false;
    }catch (Exception ex){
      throw WebinCliException.systemError(WebinCliMessage.SAMPLE_SERVICE_SYSTEM_ERROR.format(sampleIdOrAlias));
    }
  }
  
  private SampleService getSampleService(){
    return 
            new SampleService.Builder()
                    .setWebinRestV1Uri(RemoteServiceUrlHelper.getWebinRestV1Url(parameters.isTest()))
                    .setCredentials(parameters.getWebinServiceUserName(), parameters.getPassword())
                    .setWebinAuthUri(RemoteServiceUrlHelper.getWebinAuthUrl(parameters.isTest()))
                    .setBiosamplesUri(RemoteServiceUrlHelper.getBiosamplesUrl(parameters.isTest()))
                    .setBiosamplesWebinUserName(parameters.getWebinServiceUserName())
                    .setBiosamplesWebinPassword(parameters.getPassword())
                    .build();
  }
  
  private SubmitService getSubmitService(){
    
    return  new SubmitService.Builder()
            .setWebinRestV2Uri(RemoteServiceUrlHelper.getWebinRestV2Url(parameters.isTest()))
            .setUserName(parameters.getWebinServiceUserName())
            .setPassword(parameters.getPassword())
            .build();
    
  }
  
  
  private boolean isJsonSample(String value){
    return value.startsWith("{");
  }
  
  private static String getAlias(JsonNode sampleJson) {
    return sampleJson.get("alias").asText();
  }

  public String generateSampleSubmissionJson(String action, JsonNode sampleJson) {
    return "{\n"
            + generateSubmissionJson(action,sampleJson)
            + " ,\n"
            + "\"samples\":[\n"
            + sampleJson
            + "]\n"
            + "}";
  }

  public static String generateSubmissionJson(String action,JsonNode sampleJson) {
    return "\"submission\":{\n"
            + "   \"alias\":\""
            + "submission-"+ getAlias(sampleJson)
            + "\",\n"
            + "   \"actions\":[\n"
            + "      {\n"
            + "         \"type\":\""+action+"\"\n"
            + "      }\n"
            + "   ]\n"
            + "}";
  }
  
}
