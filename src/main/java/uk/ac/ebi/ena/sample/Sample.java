package uk.ac.ebi.ena.sample;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

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

    private final static String USER_ERROR_SAMPLE = "Unknown sample or the sample cannot be referenced by your submission account. " +
            "Samples must be submitted before they can be referenced in the submission.";

    private final static String SYSTEM_ERROR_INTERNAL = "An internal server error occurred when retrieving sample information.";
    private final static String SYSTEM_ERROR_UNAVAILABLE = "A service unavailable error occurred when retrieving sample information.";
    private final static String SYSTEM_ERROR_OTHER = "A server error occurred when retrieving sample information.";

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
                    throw new SampleException(USER_ERROR_SAMPLE + " Sample: " + sampleId + ".", WebinCliException.ErrorType.USER_ERROR);
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    throw new SampleException(SYSTEM_ERROR_INTERNAL, WebinCliException.ErrorType.SYSTEM_ERROR);
                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    throw new SampleException(SYSTEM_ERROR_UNAVAILABLE, WebinCliException.ErrorType.SYSTEM_ERROR);
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new SampleException(WebinCli.INVALID_CREDENTIALS, WebinCliException.ErrorType.USER_ERROR);
                default:
                    throw new SampleException(SYSTEM_ERROR_OTHER, WebinCliException.ErrorType.SYSTEM_ERROR);
            }
        } catch (Exception e) {
            throw new SampleException(SYSTEM_ERROR_OTHER, WebinCliException.ErrorType.SYSTEM_ERROR);
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
                throw new SampleException(USER_ERROR_SAMPLE + " Sample: " + sampleId + ".", WebinCliException.ErrorType.USER_ERROR);
            taxId = (long)jsonObject.get("taxId");
            organism = (String) jsonObject.get("organism");
            biosampleId = (String) jsonObject.get("bioSampleId");
        } catch (Exception e) {
            throw new SampleException(SYSTEM_ERROR_INTERNAL, WebinCliException.ErrorType.SYSTEM_ERROR);
        }
    }
}
