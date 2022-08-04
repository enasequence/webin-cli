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
package uk.ac.ebi.ena.webin.cli.service;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.service.handler.DefaultErrorHander;
import uk.ac.ebi.ena.webin.cli.service.utils.HttpHeaderBuilder;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;

public class SubmitService extends WebinService {

    private final static String SUBMISSION_XML_NAME = "webin-submission.xml";
    private final static String RECEIPT_XML_NAME = "receipt.xml";

    private static final Logger log = LoggerFactory.getLogger(SubmitService.class);
    private final String submitDir;

    private final boolean saveSubmissionXmlFiles;

    public static class Builder extends AbstractBuilder<SubmitService> {
        private String submitDir;

        private boolean saveSubmissionXmlFiles = true;
        
        public Builder setSubmitDir( String submitDir ) {
            this.submitDir = submitDir;
            return this;
        }

        public Builder setSaveSubmissionXmlFiles(boolean saveSubmissionXmlFiles) {
            this.saveSubmissionXmlFiles = saveSubmissionXmlFiles;
            return this;
        }
        
        @Override
        public SubmitService build() {
            return new SubmitService( this );
        }
    }
    
    
    protected SubmitService( Builder builder ) {
        super( builder );
        this.submitDir = builder.submitDir;
        this.saveSubmissionXmlFiles = builder.saveSubmissionXmlFiles;
    }
    

    public void
    doSubmission(List<SubmissionBundle.SubmissionXMLFile> xmlFileList) {
        String submissionXml = createSubmissionXml(xmlFileList);
        if (saveSubmissionXmlFiles) {
            saveToFile(Paths.get(submitDir, SUBMISSION_XML_NAME), submissionXml);
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(submissionXml.getBytes(StandardCharsets.UTF_8)) {
            //The remote endpoint responds back with 400 status code if file name is not present in
            //content-disposition header. overriding the following method this way adds the file name in
            //the header allowing the submission to get accepted.
            @Override
            public String getFilename() {
                return SUBMISSION_XML_NAME;
            }
        });

        HttpHeaders headers = new HttpHeaderBuilder().basicAuth( getUserName(), getPassword() ).multipartFormData().build();

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultErrorHander(WebinCliMessage.SUBMIT_SERVICE_SYSTEM_ERROR.text()));
        ResponseEntity<String> response = restTemplate.exchange(
            getWebinRestSubmissionUri( "submit/", getTest() ),
            HttpMethod.POST,
            new HttpEntity<>( body, headers),
            String.class);

        processReceipt(response.getBody(), xmlFileList);
    }

    private String createSubmissionXml(List<SubmissionBundle.SubmissionXMLFile> xmlFileList) {
        StringBuilder sb = new StringBuilder(32768);

        sb.append("<WEBIN>" + System.lineSeparator());
        xmlFileList.forEach(xmlFile -> {
            sb.append(xmlFile.getXmlContent());
            sb.append(System.lineSeparator());
        });
        sb.append("</WEBIN>");

        return sb.toString();
    }

    private void processReceipt(String receiptXml, List<SubmissionBundle.SubmissionXMLFile> xmlFileList) {
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

            if( Boolean.valueOf( rootNode.getAttributeValue( "success" ) ) ) {
                for (SubmissionBundle.SubmissionXMLFile xmlFile : xmlFileList ) {
                    //Do not show submission accession in the output.
                    if (xmlFile.getType() == SubmissionBundle.SubmissionXMLFileType.SUBMISSION) {
                        continue;
                    }

                    String xmlFileType = String.valueOf( xmlFile.getType() );
                    String accession = rootNode.getChild( xmlFileType ).getAttributeValue( "accession" );

                    String msg = ( getTest() ? WebinCliMessage.SUBMIT_SERVICE_SUCCESS_TEST
                                             : WebinCliMessage.SUBMIT_SERVICE_SUCCESS).format( xmlFileType.toLowerCase(), accession );
                   
                    if( null == accession || accession.isEmpty() ) {
                        msg = (getTest() ? WebinCliMessage.SUBMIT_SERVICE_SUCCESS_TEST_NOACC
                            : WebinCliMessage.SUBMIT_SERVICE_SUCCESS_NOACC).format(xmlFileType.toLowerCase());
                    }
                    
                    log.info( msg );
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
                throw WebinCliException.systemError(errorsSb.toString());
            }
        } catch (IOException | JDOMException ex) {
            throw WebinCliException.systemError(ex);
        }
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
