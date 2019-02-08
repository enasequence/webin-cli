/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.service;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.ena.service.handler.DefaultErrorHander;
import uk.ac.ebi.ena.service.utils.HttpHeaderBuilder;
import uk.ac.ebi.ena.submit.SubmissionBundle.SubmissionXMLFile;
import uk.ac.ebi.ena.webin.cli.WebinCliConfig;
import uk.ac.ebi.ena.webin.cli.WebinCliReporter;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class
SubmitService {
    private final static String RECEIPT_XML = "receipt.xml";
    private final String userName;
    private final String password;
    private final boolean test;
    private final String submitDir;

    final static String SYSTEM_ERROR = "IgnoreErrorsServiceSystemError";

    public SubmitService(WebinCli.Params params, String submitDir) {
        this.test = params.test;
        this.userName = params.userName;
        this.password = params.password;
        this.submitDir = submitDir;
    }

    public void
    doSubmission(List<SubmissionXMLFile> xmlFileList, String centerName, String submissionTool) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultErrorHander(WebinCliConfig.getServiceMessage(SYSTEM_ERROR)));
        // restTemplate.setInterceptors(Collections.singletonList(new HttpLoggingInterceptor()));
        // restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (SubmissionXMLFile xmlFile : xmlFileList) {
            String xmlFileType = String.valueOf(xmlFile.getType());
            body.add(xmlFileType, new FileSystemResource(xmlFile.getFile()));
        }

        body.add("ACTION", "ADD");

        if (null != centerName && !centerName.isEmpty()) {
            body.add("CENTER_NAME", centerName);
        }

        if (null != submissionTool && !submissionTool.isEmpty()) {
            body.add("ENA_SUBMISSION_TOOL", submissionTool);
        }

        HttpHeaders headers = new HttpHeaderBuilder().basicAuth(userName, password).multipartFormData().build();

        ResponseEntity<String> response = restTemplate.exchange(
                WebinCliConfig.getWebinRestUri("submit/", test),
                HttpMethod.POST,
                new HttpEntity( body, headers),
                String.class);

        processReceipt(response.getBody(), xmlFileList);
    }

    private void processReceipt(String receiptXml, List<SubmissionXMLFile> xmlFileList) {
        StringBuilder errorsSb = new StringBuilder();
        try {
            Path receiptFile = Paths.get(submitDir + File.separator + RECEIPT_XML);
            if (Files.exists(receiptFile)) {
                Files.delete(receiptFile);
            }
            Files.createFile(receiptFile);
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new StringReader(receiptXml));
            XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat(Format.getPrettyFormat());
            StringWriter stringWriter = new StringWriter();
            xmlOutput.output(doc, stringWriter);
            Files.write(receiptFile, stringWriter.toString().getBytes());
            Element rootNode = doc.getRootElement();
            if (Boolean.valueOf(rootNode.getAttributeValue("success"))) {
                for (SubmissionXMLFile xmlFile : xmlFileList ) {
                    String xmlFileType = String.valueOf(xmlFile.getType());
                    String accession = rootNode.getChild(xmlFileType).getAttributeValue("accession");
                    if (accession != null && !accession.isEmpty()) {
                        String msg = WebinCli.SUBMIT_SUCCESS + " The following " + xmlFileType.toLowerCase() + " accession was assigned to the submission: " + accession;
                        WebinCliReporter.writeToConsole(Severity.INFO, msg);
                        WebinCliReporter.writeToFile(WebinCliReporter.getDefaultReport(), Severity.INFO, msg);
                    } else {
                        String msg = WebinCli.SUBMIT_SUCCESS + " No accession was assigned to the " + xmlFileType.toLowerCase() + " XML submission. Please contact the helpdesk.";
                        WebinCliReporter.writeToConsole(Severity.INFO, msg);
                        WebinCliReporter.writeToFile(WebinCliReporter.getDefaultReport(), Severity.INFO, msg);
                    }
                }
            } else {
                List<Element> childrenList = rootNode.getChildren("MESSAGES");
                for (Element child : childrenList) {
                    List<Element> errorList = child.getChildren("ERROR");
                    if (errorList != null && !errorList.isEmpty()) {
                        errorList.stream().forEach(e -> errorsSb.append(e.getValue()));
                    }
                    else {
                        errorsSb.append("The submission failed because of an XML submission error.");
                    }
                }
            }
            if (errorsSb.length() != 0) {
                throw WebinCliException.createSystemError(errorsSb.toString());
            }
        } catch (IOException | JDOMException e) {
            throw WebinCliException.createSystemError(e.getMessage());
        }
    }
}
