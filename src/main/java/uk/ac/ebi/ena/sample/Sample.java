package uk.ac.ebi.ena.sample;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
    private String biosampleId;
    private final static String ERROR_01 = "Unknown sample or cannot be referenced by this submission account";
    private final static String ERROR_02 = "An internal error occurred, please try again later";
    private final static String ERROR_03 = "Unable to check sample at this time, please try again later";
    private final static String ERROR_04 = "Invalid username and/or password provided";
    private final static String ERROR_05 = "Unknown error occurred during submissione, please try again later";

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
                    throw new SampleException(ERROR_01);
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    throw new SampleException(ERROR_02);
                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    throw new SampleException(ERROR_03);
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new SampleException(ERROR_04);
                default:
                    throw new SampleException(ERROR_05);
            }
        } catch (Exception e) {
            throw new SampleException(ERROR_05);
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
  	
    private void extractResults(String result, String sampleId) throws SampleException {
        try {
            JSONParser jsonParser = new JSONParser();
            StringReader reader = new StringReader(result);
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
            boolean canBeReferenced = (boolean)jsonObject.get("canBeReferenced");
            if (!canBeReferenced)
                throw new SampleException(ERROR_01);
            taxId = (long)jsonObject.get("taxId");
            organism = (String) jsonObject.get("organism");
            biosampleId = (String) jsonObject.get("bioSampleId");
        } catch (Exception e) {
            throw new SampleException(ERROR_02);
        }
    }
}
