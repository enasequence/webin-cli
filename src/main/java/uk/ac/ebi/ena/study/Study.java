package uk.ac.ebi.ena.study;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import uk.ac.ebi.ena.sample.SampleException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class Study {
	private  List<String> locusTagsList = new ArrayList<>();
    private String projectId;
    private final static String ERROR_01 = "Unknown study or cannot be referenced by this submission account";
    private final static String ERROR_02 = "An internal error occurred, please try again later";
    private final static String ERROR_03 = "Unable to check study at this time, please try again later";
    private final static String ERROR_04 = "Invalid username and/or password provided";
    private final static String ERROR_05 = "Unknown error occurred during submissione, please try again later";

    public void getStudy(String studyId, String userName, String password) throws StudyException {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet("http://ves-ebi-5a:8110/ena/submit/drop-box/reference/project/" + studyId.trim());
            String encoding = Base64.getEncoder().encodeToString((userName + ":" + password).getBytes());
            httpGet.setHeader("Authorization", "Basic " + encoding);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            int responsecode = response.getStatusLine().getStatusCode();
            switch (responsecode) {
                case HttpStatus.SC_OK:
                    List<String> resultsList = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
                    String result = resultsList.stream()
                            .collect(Collectors.joining(" "));
                    extractResults(result, studyId);
                    break;
                case HttpStatus.SC_BAD_REQUEST:
                case HttpStatus.SC_NOT_FOUND:
                    throw new StudyException(ERROR_01);
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    throw new StudyException(ERROR_02);
                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    throw new StudyException(ERROR_03);
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new StudyException(ERROR_04);
                default:
                    throw new StudyException(ERROR_05);
            }
        } catch (Exception e) {
            throw new StudyException(ERROR_05);
        }
    }

    public List<String> getLocusTagsList() {
        return locusTagsList;
    }
    
    public String getProjectId()
    {
    	return projectId;
    }

    public void setLocusTagsList(List<String> locusTagsList) {
		this.locusTagsList = locusTagsList;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

    private void extractResults(String result, String studyId) throws SampleException {
        try {
            JSONParser jsonParser = new JSONParser();
            StringReader reader = new StringReader(result);
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
            boolean canBeReferenced = (boolean)jsonObject.get("canBeReferenced");
            if (!canBeReferenced)
                throw new StudyException("Unknown study " + studyId + " or cannot be referenced by this submission account.");
            JSONArray jsonArray = (JSONArray)jsonObject.get("locusTags");
            projectId = (String) jsonObject.get("bioProjectId");
            if (jsonArray != null && !jsonArray.isEmpty())
                jsonArray.forEach(p -> locusTagsList.add( p.toString()));
        } catch (Exception e) {
            throw new SampleException("An internal error occurred, please try again later. " + e.getMessage());
        }
    }
}
