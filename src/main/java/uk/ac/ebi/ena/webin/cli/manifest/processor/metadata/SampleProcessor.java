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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.client.HttpClientErrorException;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldGroup;
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
import uk.ac.ebi.ena.webin.xml.conversion.json.model.WebinSubmission;
import uk.ac.ebi.ena.webin.xml.conversion.json.model.submission.Submission;
import uk.ac.ebi.ena.webin.xml.conversion.json.model.submission.action.Action;
import uk.ac.ebi.ena.webin.xml.conversion.json.model.submission.action.ActionType;

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
  public void process(
      ValidationResult result, ManifestFieldGroup fieldGroup, ManifestFieldValue fieldValue) {
    String sampleValue = fieldValue.getValue();
    try {
      if (isJsonValue(sampleValue)) {
        uk.ac.ebi.ena.webin.xml.conversion.json.model.sample.Sample jsonSample =
            getJsonSample(sampleValue);
        Submission submission = createSubmission(jsonSample);
        WebinSubmission webinSubmission = createWebinSubmission(jsonSample, submission);

        // There will be only one action for a sample
        ActionType actionType = submission.getActions().get(0).getType();
        if (actionType == ActionType.ADD
            || (actionType == ActionType.MODIFY && parameters.isSampleUpdate())) {
          getSubmitService().doJsonSubmission(webinSubmission);
        }
        // Replace sample JSON with sample alias.
        sampleValue = jsonSample.getAlias();
      }

      Sample sample = getSample(sampleValue);
      fieldValue.setValue(sample.getBioSampleId());
      callback.notify(fieldGroup, sample);

    } catch (WebinCliException | JsonProcessingException e) {
      result.add(ValidationMessage.error(e));
    }
  }

  private WebinSubmission createWebinSubmission(
      uk.ac.ebi.ena.webin.xml.conversion.json.model.sample.Sample sample, Submission submission) {
    List<uk.ac.ebi.ena.webin.xml.conversion.json.model.sample.Sample> sampleList = new ArrayList();
    sampleList.add(sample);
    WebinSubmission webinSubmission = new WebinSubmission();
    webinSubmission.setSubmission(submission);
    webinSubmission.setSamples(sampleList);
    return webinSubmission;
  }

  private uk.ac.ebi.ena.webin.xml.conversion.json.model.sample.Sample getJsonSample(
      String sampleValue) throws JsonProcessingException {
    uk.ac.ebi.ena.webin.xml.conversion.json.model.sample.Sample sampleFromUser =
        new ObjectMapper()
            .readValue(
                sampleValue, uk.ac.ebi.ena.webin.xml.conversion.json.model.sample.Sample.class);
    return sampleFromUser;
  }

  private Submission createSubmission(
      uk.ac.ebi.ena.webin.xml.conversion.json.model.sample.Sample sample) {
    Submission submission = new Submission();
    List<Action> actions = new ArrayList<>();
    Action action =
        isSample(sample) ? createAction(ActionType.MODIFY) : createAction(ActionType.ADD);
    actions.add(action);
    submission.setActions(actions);
    return submission;
  }

  private Action createAction(ActionType type) {
    Action action = new Action();
    action.setType(type);
    return action;
  }

  private Sample getSample(String sampleIdOrAlias) {

    return ExceptionUtils.executeWithRestExceptionHandling(
        () -> getSampleService().getSample(sampleIdOrAlias),
        WebinCliMessage.SERVICE_AUTHENTICATION_ERROR.format(SampleService.SERVICE_NAME),
        WebinCliMessage.SAMPLE_SERVICE_VALIDATION_ERROR.format(sampleIdOrAlias),
        WebinCliMessage.SAMPLE_SERVICE_SYSTEM_ERROR.format(sampleIdOrAlias));
  }

  /** Return true if the sample being submitters already exists in the submission account. */
  private boolean isSample(uk.ac.ebi.ena.webin.xml.conversion.json.model.sample.Sample sample) {
    try {
      if (null == sample.getAlias()) {
        throw WebinCliException.userError(
            WebinCliMessage.MANIFEST_READER_MISSING_SAMPLE_ALIAS.format(sample.getAlias()));
      }

      return getSampleService().getSample(sample.getAlias()) != null;
    } catch (HttpClientErrorException.NotFound notFound) {
      return false;
    } catch (Exception ex) {
      throw WebinCliException.systemError(
          WebinCliMessage.SAMPLE_SERVICE_SYSTEM_ERROR.format(sample.getAlias()));
    }
  }

  private SampleService getSampleService() {
    return new SampleService.Builder()
        .setWebinRestV1Uri(RemoteServiceUrlHelper.getWebinRestV1Url(parameters.isTest()))
        .setCredentials(parameters.getWebinServiceUserName(), parameters.getPassword())
        .setWebinAuthUri(RemoteServiceUrlHelper.getWebinAuthUrl(parameters.isTest()))
        .setBiosamplesUri(RemoteServiceUrlHelper.getBiosamplesUrl(parameters.isTest()))
        .setBiosamplesWebinUserName(parameters.getWebinServiceUserName())
        .setBiosamplesWebinPassword(parameters.getPassword())
        .build();
  }

  private SubmitService getSubmitService() {

    return new SubmitService.Builder()
        .setWebinRestV2Uri(RemoteServiceUrlHelper.getWebinRestV2Url(parameters.isTest()))
        .setUserName(parameters.getWebinServiceUserName())
        .setPassword(parameters.getPassword())
        .build();
  }

  private boolean isJsonValue(String value) {
    return value.startsWith("{");
  }

  private static String getAlias(JsonNode sampleJson) {
    return sampleJson.get("alias").asText();
  }
}
