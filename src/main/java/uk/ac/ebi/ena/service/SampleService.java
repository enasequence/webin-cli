package uk.ac.ebi.ena.service;

import lombok.Data;
import org.springframework.http.HttpEntity;
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
import uk.ac.ebi.ena.entity.Sample;
import uk.ac.ebi.ena.service.handler.SampleServiceErrorHandler;
import uk.ac.ebi.ena.service.utils.HttpUtils;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

public class SampleService {

    @Data
    private static class SampleResponse {
        private long taxId;
        private String organism;
        private String bioSampleId;
        private boolean canBeReferenced;
    }

    public final static String VALIDATION_ERROR = "Unknown sample or the sample cannot be referenced by your submission account. " +
            "Samples must be submitted before they can be referenced in the submission. Sample: ";

    public final static String SYSTEM_ERROR = "A server error occurred when retrieving sample information. ";

    private static String getSampleUri(boolean test) {
        return (test) ?
                "https://wwwdev.ebi.ac.uk/ena/submit/drop-box/reference/sample/{id}" :
                "https://www.ebi.ac.uk/ena/submit/drop-box/reference/sample/{id}";
    }

    private static String getSourceFeatureUri(boolean test) {
        return (test) ?
                "https://wwwdev.ebi.ac.uk/ena/submit/drop-box/samples/{id}" :
                "https://www.ebi.ac.uk/ena/submit/drop-box/samples/{id}";
    }

    public Sample getSample(String sampleId, String userName, String password, boolean test) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new SampleServiceErrorHandler(sampleId, VALIDATION_ERROR, SYSTEM_ERROR));

        ResponseEntity<SampleResponse> response = restTemplate.exchange(
                getSampleUri(test),
                HttpMethod.GET,
                new HttpEntity(HttpUtils.authHeader(userName, password)),
                SampleResponse.class,
                sampleId.trim());

        SampleResponse sampleResponse = response.getBody();
        if (sampleResponse == null || !sampleResponse.isCanBeReferenced()) {
            throw WebinCliException.createUserError(VALIDATION_ERROR, sampleId);
        }
        Sample sample = new Sample();
        sample.setBiosampleId(sampleResponse.getBioSampleId());
        sample.setTaxId(sampleResponse.getTaxId());
        sample.setOrganism(sampleResponse.getOrganism());
        return sample;
    }


    public static SourceFeature
    getSourceFeature(String sampleId, String userName, String password, boolean test) {

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new SampleServiceErrorHandler(sampleId, VALIDATION_ERROR, SYSTEM_ERROR));

        ResponseEntity<String> response = restTemplate.exchange(
                getSourceFeatureUri(test),
                HttpMethod.GET,
                new HttpEntity(HttpUtils.authHeader(userName, password)),
                String.class,
                sampleId.trim());

        return getSourceFeature(sampleId, response.getBody());
    }

    private static SourceFeature
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
            throw WebinCliException.createUserError(VALIDATION_ERROR, sampleId);
        }
    }
}
