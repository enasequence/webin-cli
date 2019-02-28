package uk.ac.ebi.ena.service;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import uk.ac.ebi.embl.api.entry.feature.FeatureFactory;
import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.embl.api.entry.qualifier.Qualifier;
import uk.ac.ebi.embl.api.validation.helper.MasterSourceFeatureUtils;
import uk.ac.ebi.embl.api.validation.helper.taxon.TaxonHelperImpl;
import uk.ac.ebi.ena.service.handler.NotFoundErrorHandler;
import uk.ac.ebi.ena.service.utils.HttpHeaderBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;

public class 
SourceFeatureService extends AbstractService
{
    protected 
    SourceFeatureService( AbstractBuilder<SourceFeatureService> builder )
    {
        super( builder );
    }

    public static class 
    Builder extends AbstractBuilder<SourceFeatureService>
    {
        @Override public SourceFeatureService
        build()
        {
            return new SourceFeatureService( this );
        }
    };

    public SourceFeature 
    getSourceFeature( String sampleId )
    {
        return getSourceFeature( sampleId, getUserName(), getPassword(), getTest() );
    }

    
    private SourceFeature
    getSourceFeature( String sampleId, String userName, String password, boolean test ) {

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new NotFoundErrorHandler(
                WebinCliMessage.Service.SAMPLE_SERVICE_VALIDATION_ERROR.format(sampleId),
                WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.format(sampleId)));

        HttpHeaders headers = new HttpHeaderBuilder().basicAuth(userName, password).build();

        ResponseEntity<String> response = restTemplate.exchange(
                getWebinRestUri("samples/{id}", test),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class,
                sampleId.trim());

        return getSourceFeature(sampleId, response.getBody());
    }

    
    private SourceFeature
    getSourceFeature(String sampleId, String sampleXml) {
        MasterSourceFeatureUtils sourceUtils = new MasterSourceFeatureUtils();
        SourceFeature sourceFeature = new FeatureFactory().createSourceFeature();

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(sampleXml)));
            doc.getDocumentElement().normalize();

            // alias
            String alias = null;
            NodeList sampleList = doc.getElementsByTagName("SAMPLE");
            for (int temp = 0; temp < sampleList.getLength(); temp++) {
                Node nNode = sampleList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    alias = eElement.getAttribute("alias");
                }
            }

            //organism, taxid
            NodeList sampleNameList = doc.getElementsByTagName("SAMPLE_NAME");
            for (int temp = 0; temp < sampleNameList.getLength(); temp++) {
                Node nNode = sampleNameList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    sourceFeature.addQualifier(Qualifier.DB_XREF_QUALIFIER_NAME, eElement.getElementsByTagName("TAXON_ID").item(0).getTextContent());

                    if (eElement.getElementsByTagName("SCIENTIFIC_NAME").getLength() > 0)
                        sourceFeature.setScientificName(eElement.getElementsByTagName("SCIENTIFIC_NAME").item(0).getTextContent());
                }
            }

            //source qualifiers
            NodeList attributeList = doc.getElementsByTagName("SAMPLE_ATTRIBUTE");
            for (int temp = 0; temp < attributeList.getLength(); temp++) {
                Node nNode = attributeList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String qualifierName = eElement.getElementsByTagName("TAG").item(0).getTextContent();
                    String qualifierValue = eElement.getElementsByTagName("VALUE").getLength() > 0 ? eElement.getElementsByTagName("VALUE").item(0).getTextContent()
                            : null;
                    sourceUtils.addSourceQualifier(qualifierName, qualifierValue, sourceFeature);
                }
            }

            sourceUtils.addExtraSourceQualifiers(sourceFeature, new TaxonHelperImpl(), alias);

            return sourceFeature;
        } catch (Exception ex) {
            throw WebinCliException.userError(
                    WebinCliMessage.Service.SAMPLE_SERVICE_VALIDATION_ERROR.format(sampleId));
        }
    }
}
