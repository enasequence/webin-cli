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
package uk.ac.ebi.ena.webin.cli.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.service.utils.HttpHeaderBuilder;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.utils.ExceptionUtils;
import uk.ac.ebi.ena.webin.cli.utils.RetryUtils;
import uk.ac.ebi.ena.webin.xml.conversion.json.model.WebinSubmission;
import uk.ac.ebi.ena.webin.xml.conversion.json.model.receipt.Receipt;
import uk.ac.ebi.ena.webin.xml.conversion.json.model.receipt.ReceiptObject;

public class SubmitService extends WebinService {

  private static final String SUBMISSION_XML_NAME = "webin-submission.xml";
  private static final String SUBMISSION_JSON_NAME = "webin-submission.json";
  private static final String RECEIPT_XML_NAME = "receipt.xml";

  private static final Logger log = LoggerFactory.getLogger(SubmitService.class);
  private final String submitDir;

  private final boolean saveSubmissionXmlFiles;

  public static class Builder extends AbstractBuilder<SubmitService> {
    private String submitDir;

    private boolean saveSubmissionXmlFiles = true;

    public Builder setSubmitDir(String submitDir) {
      this.submitDir = submitDir;
      return this;
    }

    public Builder setSaveSubmissionXmlFiles(boolean saveSubmissionXmlFiles) {
      this.saveSubmissionXmlFiles = saveSubmissionXmlFiles;
      return this;
    }

    @Override
    public SubmitService build() {
      return new SubmitService(this);
    }
  }

  protected SubmitService(Builder builder) {
    super(builder);
    this.submitDir = builder.submitDir;
    this.saveSubmissionXmlFiles = builder.saveSubmissionXmlFiles;
  }

  public void doSubmission(List<SubmissionBundle.SubmissionXMLFile> xmlFileList)
      throws WebinCliException {
    String submissionXml = createSubmissionXml(xmlFileList);
    if (saveSubmissionXmlFiles) {
      saveToFile(Paths.get(submitDir, SUBMISSION_XML_NAME), submissionXml);
    }

    MultiValueMap<String, Object> body = getRequestBody(submissionXml, SUBMISSION_XML_NAME);
    ResponseEntity<String> response = submit(body);
    processReceipt(response.getBody(), xmlFileList);
  }

  public void doJsonSubmission(WebinSubmission jsonSubmission) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    MultiValueMap<String, Object> body =
        getRequestBody(objectMapper.writeValueAsString(jsonSubmission), SUBMISSION_JSON_NAME);
    ResponseEntity<String> response = submit(body);
    processJsonReceipt(response);
  }

  private ResponseEntity<String> submit(MultiValueMap<String, Object> body) {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers =
        new HttpHeaderBuilder().basicAuth(getUserName(), getPassword()).multipartFormData().build();

    return ExceptionUtils.executeWithRestExceptionHandling(
        () ->
            RetryUtils.executeWithRetry(
                context ->
                    restTemplate.exchange(
                        resolveAgainstWebinRestV2Uri("submit/"),
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class),
                context -> log.warn("Retrying sending submission to server."),
                HttpServerErrorException.class,
                ResourceAccessException.class),
        WebinCliMessage.SERVICE_AUTHENTICATION_ERROR.format("Submit"),
        null,
        WebinCliMessage.SUBMIT_SAMPLE_SERVICE_SYSTEM_ERROR.text());
  }

  private MultiValueMap<String, Object> getRequestBody(String requestContent, String fileName) {

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add(
        "file",
        new ByteArrayResource(requestContent.getBytes(StandardCharsets.UTF_8)) {
          // The remote endpoint responds back with 400 status code if file name is not present in
          // content-disposition header. overriding the following method this way adds the file name
          // in
          // the header allowing the submission to get accepted.
          @Override
          public String getFilename() {
            return fileName;
          }
        });
    return body;
  }

  private String createSubmissionXml(List<SubmissionBundle.SubmissionXMLFile> xmlFileList) {
    StringBuilder sb = new StringBuilder(32768);

    sb.append("<WEBIN>" + System.lineSeparator());
    xmlFileList.forEach(
        xmlFile -> {
          sb.append(xmlFile.getXmlContent());
          sb.append(System.lineSeparator());
        });
    sb.append("</WEBIN>");

    return sb.toString();
  }

  private void processReceipt(
      String receiptXml, List<SubmissionBundle.SubmissionXMLFile> xmlFileList)
      throws WebinCliException {
    StringBuilder errorsSb = new StringBuilder();
    try {
      SAXBuilder builder = new SAXBuilder();
      Document doc = builder.build(new StringReader(receiptXml));
      XMLOutputter xmlOutput = new XMLOutputter();
      xmlOutput.setFormat(Format.getPrettyFormat());
      StringWriter stringWriter = new StringWriter();
      xmlOutput.output(doc, stringWriter);

      saveToFile(Paths.get(submitDir + File.separator + RECEIPT_XML_NAME), stringWriter.toString());

      Element rootNode = doc.getRootElement();

      if (Boolean.valueOf(rootNode.getAttributeValue("success"))) {
        for (SubmissionBundle.SubmissionXMLFile xmlFile : xmlFileList) {
          // Do not show submission accession in the output.
          if (xmlFile.getType() == SubmissionBundle.SubmissionXMLFileType.SUBMISSION) {
            continue;
          }

          String xmlFileType = String.valueOf(xmlFile.getType());
          String accession = rootNode.getChild(xmlFileType).getAttributeValue("accession");

          String msg =
              WebinCliMessage.SUBMIT_SERVICE_SUCCESS.format(xmlFileType.toLowerCase(), accession);

          if (null == accession || accession.isEmpty()) {
            msg = WebinCliMessage.SUBMIT_SERVICE_SUCCESS_NOACC.format(xmlFileType.toLowerCase());
          }

          log.info(msg);
        }
      } else {
        List<Element> childrenList = rootNode.getChildren("MESSAGES");
        for (Element child : childrenList) {
          List<Element> errorList = child.getChildren("ERROR");
          if (errorList != null && !errorList.isEmpty()) {
            errorList.stream().forEach(e -> errorsSb.append(e.getValue()));
          } else {
            errorsSb.append("The submission failed because of an XML submission error.");
          }
        }
      }

      if (errorsSb.length() != 0) {
        throw WebinCliException.systemError(errorsSb.toString());
      }
    } catch (IOException | JDOMException ex) {
      throw WebinCliException.systemError(ex);
    }
  }

  private void processJsonReceipt(ResponseEntity<String> response) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      Receipt receipt = objectMapper.readValue(response.getBody(), Receipt.class);

      if (receipt.isSuccess()) {
        Map<String, String> assignedAccessionsByObjectType =
            getAssignedAccessionsByObjectType(receipt);
        String objectTypes =
            assignedAccessionsByObjectType.keySet().stream().collect(Collectors.joining(", "));
        String assignedAccessions =
            assignedAccessionsByObjectType.values().stream().collect(Collectors.joining(", "));
        String msg = null;
        if (StringUtils.isEmpty(assignedAccessions)) {
          msg = WebinCliMessage.SUBMIT_SERVICE_SUCCESS_NOACC.format(objectTypes);
        } else {
          msg = WebinCliMessage.SUBMIT_SERVICE_SUCCESS.format(objectTypes, assignedAccessions);
        }
        log.info(msg);

      } else {
        throw WebinCliException.systemError(
            WebinCliMessage.SERVICE_JSON_SUBMISSION_ERROR.format(
                receipt.getMessages().getErrorMessages()));
      }
    } catch (JsonProcessingException e) {
      throw WebinCliException.systemError(e);
    }
  }

  private Map<String, String> getAssignedAccessionsByObjectType(Receipt receipt) {

    Map<String, String> submissionsAccessions = new HashMap();
    Map<String, List<ReceiptObject>> collectionMappings = new HashMap<>();
    collectionMappings.put("Analysis", receipt.getAnalyses());
    collectionMappings.put("Experiments", receipt.getExperiments());
    collectionMappings.put("Runs", receipt.getRuns());
    collectionMappings.put("Samples", receipt.getSamples());
    collectionMappings.put("Studies", receipt.getStudies());
    collectionMappings.put("Projects", receipt.getProjects());

    for (Map.Entry<String, List<ReceiptObject>> entry : collectionMappings.entrySet()) {
      String label = entry.getKey();
      List<ReceiptObject> objects = entry.getValue();

      if (objects != null && !objects.isEmpty()) {
        submissionsAccessions.put(
            label,
            objects.stream().map(ReceiptObject::getAccession).collect(Collectors.joining(",")));
      }
    }
    return submissionsAccessions;
  }

  private void saveToFile(Path filePath, String data) {
    try {
      if (Files.exists(filePath)) {
        Files.delete(filePath);
      }
      Files.createFile(filePath);
      Files.write(filePath, data.getBytes(StandardCharsets.UTF_8));
    } catch (IOException ex) {
      throw WebinCliException.systemError(ex);
    }
  }
}
