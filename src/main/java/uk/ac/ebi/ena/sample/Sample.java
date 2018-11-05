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
package uk.ac.ebi.ena.sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import uk.ac.ebi.embl.api.entry.feature.FeatureFactory;
import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.embl.api.entry.qualifier.Qualifier;
import uk.ac.ebi.embl.api.validation.helper.MasterSourceFeatureUtils;
import uk.ac.ebi.embl.api.validation.helper.taxon.TaxonHelperImpl;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class Sample {
	private long taxId;
    private String organism;
    private String biosampleId;
	
    private final static String VALIDATION_ERROR_SAMPLE = "Unknown sample or the sample cannot be referenced by your submission account. " +
            "Samples must be submitted before they can be referenced in the submission. Sample: ";
    private final static String SYSTEM_ERROR_INTERNAL = "An internal server error occurred when retrieving sample information. ";
    private final static String SYSTEM_ERROR_UNAVAILABLE = "A service unavailable error occurred when retrieving sample information. ";
    private final static String SYSTEM_ERROR_OTHER = "A server error occurred when retrieving sample information. ";

    public static Sample getSample(String sampleId, String userName, String password, boolean TEST)  {
        try {
            Sample sample = new Sample();
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet( ( TEST ? "https://wwwdev.ebi.ac.uk/ena/submit/drop-box/reference/sample/" : "https://www.ebi.ac.uk/ena/submit/drop-box/reference/sample/" ) +  URLEncoder.encode( sampleId.trim(), "UTF-8" ) );
            String encoding = Base64.getEncoder().encodeToString((userName + ":" + password).getBytes());
            httpGet.setHeader("Authorization", "Basic " + encoding);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            int responsecode = response.getStatusLine().getStatusCode();
            switch (responsecode) {
                case HttpStatus.SC_OK:
                    List<String> resultsList = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
                    String result = resultsList.stream()
                            .collect(Collectors.joining(" "));
                    sample.extractResults(result, sampleId);
                    return sample;
                case HttpStatus.SC_BAD_REQUEST:
                case HttpStatus.SC_NOT_FOUND:
                    throw WebinCliException.createValidationError(VALIDATION_ERROR_SAMPLE, sampleId);
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    throw WebinCliException.createSystemError(SYSTEM_ERROR_INTERNAL);
                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    throw WebinCliException.createSystemError(SYSTEM_ERROR_UNAVAILABLE);
                case HttpStatus.SC_UNAUTHORIZED:
                case HttpStatus.SC_FORBIDDEN:
                    throw WebinCliException.createUserError(WebinCli.AUTHENTICATION_ERROR);
                default:
                    throw WebinCliException.createSystemError(SYSTEM_ERROR_OTHER);
            }
        } catch( IOException e )
        {
            throw WebinCliException.createSystemError(SYSTEM_ERROR_OTHER);
        }
    }

    
    public static SourceFeature 
    getSourceFeature( String sampleId, String userName, String password, boolean TEST )
    {
        try 
        {
            String uniqueName = null;
            MasterSourceFeatureUtils sourceUtils = new MasterSourceFeatureUtils();
            SourceFeature sourceFeature = new FeatureFactory().createSourceFeature();
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet( ( TEST ? "https://wwwdev.ebi.ac.uk/ena/submit/drop-box/samples/" : "https://www.ebi.ac.uk/ena/submit/drop-box/samples/" ) +  URLEncoder.encode( sampleId.trim(), "UTF-8" ) );
            String encoding = Base64.getEncoder().encodeToString( ( userName + ":" + password ).getBytes() );
            httpGet.setHeader( "Authorization", "Basic " + encoding );
            CloseableHttpResponse response = httpClient.execute( httpGet );
            
            int responsecode = response.getStatusLine().getStatusCode();
            switch( responsecode )
            {
                case HttpStatus.SC_OK:
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    Document doc = db.parse( response.getEntity().getContent() );
                    doc.getDocumentElement().normalize();
                    
                    // alias
                    NodeList sampleList = doc.getElementsByTagName( "SAMPLE" );
                    for( int temp = 0; temp < sampleList.getLength(); temp++ ) 
                    {
                    	 Node nNode = sampleList.item( temp );
                     
                         if( nNode.getNodeType() == Node.ELEMENT_NODE ) 
                         {
                            Element eElement = (Element) nNode;
                            uniqueName=eElement.getAttribute( "alias" );
                         }
                    }
                    
                    //organism, taxid
                    NodeList sampleNameList = doc.getElementsByTagName( "SAMPLE_NAME" );
                    for( int temp = 0; temp < sampleNameList.getLength(); temp++ ) 
                    {
                    	 Node nNode = sampleNameList.item( temp );
                         if( nNode.getNodeType() == Node.ELEMENT_NODE )
                         {
                            Element eElement = (Element) nNode;
                            sourceFeature.addQualifier( Qualifier.DB_XREF_QUALIFIER_NAME, eElement.getElementsByTagName( "TAXON_ID" ).item( 0 ).getTextContent() );
                         
                            if( eElement.getElementsByTagName( "SCIENTIFIC_NAME" ).getLength() > 0 )
                                sourceFeature.setScientificName( eElement.getElementsByTagName( "SCIENTIFIC_NAME" ).item( 0 ).getTextContent() );
                         }
                    }
                    
                    //source qualifiers
                   NodeList attributeList = doc.getElementsByTagName( "SAMPLE_ATTRIBUTE" );
                   for( int temp = 0; temp < attributeList.getLength(); temp++ ) 
                   {
                        Node nNode = attributeList.item( temp );
                        if( nNode.getNodeType() == Node.ELEMENT_NODE ) 
                        {
                           Element eElement = (Element) nNode;
                           String qualifierName  = eElement.getElementsByTagName( "TAG" ).item( 0 ).getTextContent();
                           String qualifierValue = eElement.getElementsByTagName( "VALUE" ).getLength() > 0 ? eElement.getElementsByTagName( "VALUE" ).item( 0 ).getTextContent() 
                                                                                                            : null;
                           sourceUtils.addSourceQualifier( qualifierName, qualifierValue, sourceFeature );
                        }
                    }
                    
                    sourceUtils.addExtraSourceQualifiers( sourceFeature, new TaxonHelperImpl(), uniqueName );
                   
                    return sourceFeature;
                    
                case HttpStatus.SC_BAD_REQUEST:
                case HttpStatus.SC_NOT_FOUND:
                    throw WebinCliException.createValidationError( VALIDATION_ERROR_SAMPLE, sampleId );
                    
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    throw WebinCliException.createSystemError( SYSTEM_ERROR_INTERNAL );

                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    throw WebinCliException.createSystemError( SYSTEM_ERROR_UNAVAILABLE );
                
                case HttpStatus.SC_UNAUTHORIZED:
                case HttpStatus.SC_FORBIDDEN:
                    throw WebinCliException.createUserError( WebinCli.AUTHENTICATION_ERROR );
                
                default:
                    throw WebinCliException.createSystemError( SYSTEM_ERROR_OTHER );
            }
        } catch( Exception e )
        {
            throw WebinCliException.createSystemError( SYSTEM_ERROR_OTHER );
        }
    }
    
    public long getTaxId() {
        return taxId;
    }

    public String getOrganism() {
        return organism;
    }
    public String getBiosampleId() {
        return biosampleId;
    }

    public void setTaxId(long taxId) {
  		this.taxId = taxId;
  	}

  	public void setOrganism(String organism) {
  		this.organism = organism;
  	}

  	public void setBiosampleId(String biosampleId) {
  		this.biosampleId = biosampleId;
  	}
  	
    private void extractResults(String result, String sampleId) {
        try {
            JSONParser jsonParser = new JSONParser();
            StringReader reader = new StringReader(result);
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
            boolean canBeReferenced = (boolean)jsonObject.get("canBeReferenced");
            if (!canBeReferenced)
                throw WebinCliException.createUserError(VALIDATION_ERROR_SAMPLE, sampleId);
           taxId = (long)jsonObject.get("taxId");
           organism = (String) jsonObject.get("organism");
           biosampleId = (String) jsonObject.get("bioSampleId");
        } catch (IOException | ParseException e) {
            throw WebinCliException.createSystemError(SYSTEM_ERROR_INTERNAL);
        }
    }
    
}
