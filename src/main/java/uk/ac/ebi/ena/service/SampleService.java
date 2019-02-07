package uk.ac.ebi.ena.service;

import lombok.Data;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.ebi.embl.api.entry.feature.FeatureFactory;
import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.embl.api.entry.qualifier.Qualifier;
import uk.ac.ebi.embl.api.validation.helper.MasterSourceFeatureUtils;
import uk.ac.ebi.embl.api.validation.helper.taxon.TaxonHelperImpl;
import uk.ac.ebi.ena.entity.Sample;
import uk.ac.ebi.ena.service.handler.SampleServiceErrorHandler;
import uk.ac.ebi.ena.service.utils.HttpUtils;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Base64;

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

    private static String getUri(boolean test) {
        return (test) ?
                "https://wwwdev.ebi.ac.uk/ena/submit/drop-box/reference/sample/{id}" :
                "https://www.ebi.ac.uk/ena/submit/drop-box/reference/sample/{id}";
    }

    public Sample getSample(String sampleId, String userName, String password, boolean test) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new SampleServiceErrorHandler(sampleId, VALIDATION_ERROR, SYSTEM_ERROR));

        ResponseEntity<SampleResponse> response = restTemplate.exchange(
                getUri(test),
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

        /*
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet((test ? "https://wwwdev.ebi.ac.uk/ena/submit/drop-box/reference/sample/" : "https://www.ebi.ac.uk/ena/submit/drop-box/reference/sample/") + URLEncoder.encode(sampleId.trim(), "UTF-8"));
            String encoding = Base64.getEncoder().encodeToString((userName + ":" + password).getBytes());
            httpGet.setHeader("Authorization", "Basic " + encoding);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            int responsecode = response.getStatusLine().getStatusCode();
            switch (responsecode) {
                case HttpStatus.SC_OK:
                    List<String> resultsList = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
                    String result = resultsList.stream()
                            .collect(Collectors.joining(" "));
                    return createSample(result, sampleId);
                case HttpStatus.SC_BAD_REQUEST:
                case HttpStatus.SC_NOT_FOUND:
                    throw WebinCliException.createValidationError(VALIDATION_ERROR, sampleId);
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    throw WebinCliException.createSystemError(SYSTEM_ERROR_INTERNAL);
                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    throw WebinCliException.createSystemError(SYSTEM_ERROR_UNAVAILABLE);
                case HttpStatus.SC_UNAUTHORIZED:
                case HttpStatus.SC_FORBIDDEN:
                    throw WebinCliException.createUserError(WebinCli.AUTHENTICATION_ERROR);
                default:
                    throw WebinCliException.createSystemError(SYSTEM_ERROR);
            }
        } catch (IOException e) {
            throw WebinCliException.createSystemError(SYSTEM_ERROR);
        }
        */
    }

    public static SourceFeature
    getSourceFeature(String sampleId, String userName, String password, boolean TEST) {
        try {
            String uniqueName = null;
            MasterSourceFeatureUtils sourceUtils = new MasterSourceFeatureUtils();
            SourceFeature sourceFeature = new FeatureFactory().createSourceFeature();
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet((TEST ? "https://wwwdev.ebi.ac.uk/ena/submit/drop-box/samples/" : "https://www.ebi.ac.uk/ena/submit/drop-box/samples/") + URLEncoder.encode(sampleId.trim(), "UTF-8"));
            String encoding = Base64.getEncoder().encodeToString((userName + ":" + password).getBytes());
            httpGet.setHeader("Authorization", "Basic " + encoding);
            CloseableHttpResponse response = httpClient.execute(httpGet);

            int responsecode = response.getStatusLine().getStatusCode();
            switch (responsecode) {
                case HttpStatus.SC_OK:
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    Document doc = db.parse(response.getEntity().getContent());
                    doc.getDocumentElement().normalize();

                    // alias
                    NodeList sampleList = doc.getElementsByTagName("SAMPLE");
                    for (int temp = 0; temp < sampleList.getLength(); temp++) {
                        Node nNode = sampleList.item(temp);

                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element eElement = (Element) nNode;
                            uniqueName = eElement.getAttribute("alias");
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

                    sourceUtils.addExtraSourceQualifiers(sourceFeature, new TaxonHelperImpl(), uniqueName);

                    return sourceFeature;

                case HttpStatus.SC_BAD_REQUEST:
                case HttpStatus.SC_NOT_FOUND:
                    throw WebinCliException.createValidationError(VALIDATION_ERROR, sampleId);

                //case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                //    throw WebinCliException.createSystemError(SYSTEM_ERROR_INTERNAL);

                //case HttpStatus.SC_SERVICE_UNAVAILABLE:
                //    throw WebinCliException.createSystemError(SYSTEM_ERROR_UNAVAILABLE);

                case HttpStatus.SC_UNAUTHORIZED:
                case HttpStatus.SC_FORBIDDEN:
                    throw WebinCliException.createUserError(WebinCli.AUTHENTICATION_ERROR);

                default:
                    throw WebinCliException.createSystemError(SYSTEM_ERROR);
            }
        } catch (IOException | ParserConfigurationException | UnsupportedOperationException | SAXException e) {
            throw WebinCliException.createSystemError(SYSTEM_ERROR);
        }
    }


    /*
    private static Sample createSample(String result, String sampleId) {
        Sample sample = new Sample();
        try {
            JSONParser jsonParser = new JSONParser();
            StringReader reader = new StringReader(result);
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
            boolean canBeReferenced = (boolean) jsonObject.get("canBeReferenced");
            if (!canBeReferenced)
                throw WebinCliException.createUserError(VALIDATION_ERROR, sampleId);
            sample.setTaxId((long) jsonObject.get("taxId"));
            sample.setOrganism((String) jsonObject.get("organism"));
            sample.setBiosampleId((String) jsonObject.get("bioSampleId"));
            return sample;
        } catch (IOException | ParseException e) {
            throw WebinCliException.createSystemError(SYSTEM_ERROR_INTERNAL);
        }
    }
    */

}
