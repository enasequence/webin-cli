package uk.ac.ebi.ena.sample;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class Sample {
    private long taxId;
    private String organism;

    public void getSample(String sampleId, String userName, String password) throws SampleException {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet("http://ves-ebi-5a:8110/ena/submit/drop-box/reference/sample/" + sampleId.trim());
            String encoding = Base64.getEncoder().encodeToString((userName + ":" + password).getBytes());
            httpGet.setHeader("Authorization", "Basic " + encoding);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            int responsecode = response.getStatusLine().getStatusCode();
            switch (responsecode) {
                case HttpStatus.SC_OK:
                    List<String> resultsList = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
                    String result = resultsList.stream()
                            .collect(Collectors.joining(" "));
                    extractResults(result, sampleId);
                    break;
                case HttpStatus.SC_BAD_REQUEST:
                case HttpStatus.SC_NOT_FOUND:
                    throw new SampleException("Unknown sample " + sampleId + " or cannot be referenced by this submission account.");
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    throw new SampleException("An internal error occurred, please try again later.");
                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    throw new SampleException("Unable to check sample at this time, please try again later.");
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new SampleException("Invalis username and/or password provided.");
                default:
                    throw new SampleException("Invalis username and/or password provided.");
            }
        } catch (Exception e) {
            throw new SampleException("An internal error occurred, please try again later. " + e.getMessage());
        }
    }

    public long getTaxId() {
        return taxId;
    }

    public String getOrganism() {
        return organism;
    }

    private void extractResults(String result, String sampleId) throws SampleException {
        try {
            JSONParser jsonParser = new JSONParser();
            StringReader reader = new StringReader(result);
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
            boolean canBeReferenced = (boolean)jsonObject.get("canBeReferenced");
            if (!canBeReferenced)
                throw new SampleException("Unknown sample " + sampleId + " or cannot be referenced by this submission account.");
            taxId = (long)jsonObject.get("taxId");
            organism = (String) jsonObject.get("organism");
        } catch (Exception e) {
            throw new SampleException("An internal error occurred, please try again later. " + e.getMessage());
        }
        System.out.println(result);
    }
}
