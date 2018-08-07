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

package uk.ac.ebi.ena.submit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.ena.submit.SubmissionBundle.SubmissionXMLFile;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class 
Submit 
{
    private final static String RECEIPT_XML = "receipt.xml";
    private String userName;
    private String password;
    private ContextE contextE;
    private String study;
    private String sample;
    private String assemblyName;
    private final boolean TEST;
    private String submitDir;
    private String centerName;
    private String submission_tool;

    private final static String SYSTEM_ERROR_INTERNAL = "An internal server error occurred when attempting to submit. ";
    private final static String SYSTEM_ERROR_UNAVAILABLE = "A service unavailable error occurred when attempting to submit. ";
    private final static String SYSTEM_ERROR_BAD_REQUEST = "A bad request error occurred when attempting to submit. ";
    private final static String SYSTEM_ERROR_OTHER = "A server error occurred when when attempting to submit. ";

    public Submit(WebinCli.Params params, String submitDir, AssemblyInfoEntry assemblyInfoEntry, String submission_tool ) {
        try {
            this.contextE = ContextE.valueOf(params.context);
        } catch (IllegalArgumentException e) {
            throw WebinCliException.createUserError(WebinCli.INVALID_CONTEXT, params.context);
        }
        this.TEST = params.test;
        this.userName = params.userName;
        this.password = params.password;
        this.submitDir = submitDir;
        this.study = assemblyInfoEntry.getStudyId();
        this.sample = assemblyInfoEntry.getSampleId();
        this.assemblyName = assemblyInfoEntry.getName().trim().replaceAll("\\s+", "_");
        this.centerName = params.centerName;
        this.submission_tool = submission_tool;
    }
           
    
    public void 
    doSubmission( List<SubmissionXMLFile> payload_list, String centerName, String submission_tool ) 
    {
        try
        {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost( TEST ? "https://wwwdev.ebi.ac.uk/ena/submit/drop-box/submit/" : "https://www.ebi.ac.uk/ena/submit/drop-box/submit/" );
 
            String encoding = Base64.getEncoder().encodeToString( ( userName + ":" + password ).getBytes() );
            httpPost.setHeader( "Authorization", "Basic " + encoding );
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            
            for( SubmissionXMLFile p: payload_list )
                builder.addBinaryBody( String.valueOf( p.getType() ), new FileInputStream( p.getFile() ), ContentType.APPLICATION_OCTET_STREAM, p.getFile().getName() );

            builder.addTextBody( "ACTION", "ADD" );                
            
            if( null != centerName && !centerName.isEmpty() )
                builder.addTextBody( "CENTER_NAME", centerName );
           
            if( null != submission_tool && !submission_tool.isEmpty() )
                builder.addTextBody( "ENA_SUBMISSION_TOOL", submission_tool );

            HttpEntity multipart = builder.build();
            httpPost.setEntity( multipart );
            CloseableHttpResponse response = httpClient.execute( httpPost );
            int responsecode = response.getStatusLine().getStatusCode();
            List<String> resultsList = new BufferedReader( new InputStreamReader( response.getEntity().getContent(), StandardCharsets.UTF_8 ) ).lines().collect(Collectors.toList() );
            switch( responsecode )
            {
                case HttpStatus.SC_OK:
                    payload_list.forEach( e -> extractReceipt( resultsList, String.valueOf( e.getType() ) ) );
                    break;
                    
                case HttpStatus.SC_UNAUTHORIZED:
                    throw WebinCliException.createUserError( WebinCli.AUTHENTICATION_ERROR );
                    
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    throw WebinCliException.createSystemError( SYSTEM_ERROR_INTERNAL );
                    
                case HttpStatus.SC_BAD_REQUEST:
                    throw WebinCliException.createSystemError( SYSTEM_ERROR_BAD_REQUEST );
                    
                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    throw WebinCliException.createSystemError( SYSTEM_ERROR_UNAVAILABLE );
                    
                default:
                    throw WebinCliException.createSystemError( SYSTEM_ERROR_OTHER );
            }
        } catch( FileNotFoundException fnfe )
        {
            throw WebinCliException.createSystemError( "File missing: " + fnfe.getMessage() );
        } catch( IOException e ) 
        {
            throw WebinCliException.createSystemError( SYSTEM_ERROR_OTHER, e.getMessage() );
        }
    }

    
    private void extractReceipt(List<String> resultsList, String payloadType ) {
        StringBuilder errorsSb = new StringBuilder();
        try {
            String receipt = resultsList.stream()
                  .collect(Collectors.joining());
            Path receiptFile = Paths.get(submitDir + File.separator + RECEIPT_XML);
            if (Files.exists(receiptFile))
                Files.delete(receiptFile);
            Files.createFile(receiptFile);
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new StringReader(receipt));
            XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat(Format.getPrettyFormat());
            StringWriter stringWriter = new StringWriter();
            xmlOutput.output(doc, stringWriter);
            Files.write(receiptFile, stringWriter.toString().getBytes());
            Element rootNode = doc.getRootElement();
            if (Boolean.valueOf(rootNode.getAttributeValue("success"))) {
                String accession = rootNode.getChild( payloadType ).getAttributeValue( "accession" );
                if (accession != null && !accession.isEmpty())
                    WebinCli.writeMessage(Severity.INFO, WebinCli.SUBMIT_SUCCESS + " The following " + payloadType.toLowerCase() +" accession was assigned to the submission: " + accession);
                else
                    WebinCli.writeMessage(Severity.INFO, WebinCli.SUBMIT_SUCCESS + " No accession was assigned to the " + payloadType.toLowerCase() + " XML submission. Please contact the helpdesk.");
            } else {
                List<Element> childrenList = rootNode.getChildren("MESSAGES");
                for (Element child : childrenList) {
                    List<Element> errorList = child.getChildren("ERROR");
                    if (errorList != null && !errorList.isEmpty())
                        errorList.stream().forEach(e -> errorsSb.append(e.getValue()));
                    else
                        errorsSb.append("The submission failed because of an " + payloadType.toLowerCase() + " XML submission error.");
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
