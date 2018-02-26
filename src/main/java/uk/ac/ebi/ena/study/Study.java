package uk.ac.ebi.ena.study;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
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

    private final static String USER_ERROR_STUDY = "Unknown study (project) or the study (project) cannot be referenced by your submission account. " +
            "Studies (projects) must be submitted before they can be referenced in the submission.";

    private final static String SYSTEM_ERROR_INTERNAL = "An internal server error occurred when retrieving study information. ";
    private final static String SYSTEM_ERROR_UNAVAILABLE = "A service unavailable error occurred when retrieving study information. ";
    private final static String SYSTEM_ERROR_OTHER = "A server error occurred when retrieving study information. ";

    public void getStudy(String studyId, String userName, String password, boolean TEST) throws StudyException {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet((TEST ? "https://wwwdev.ebi.ac.uk/ena/submit/drop-box/reference/project/" : "https://www.ebi.ac.uk/ena/submit/drop-box/reference/project/") + studyId.trim());
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
                    throw new StudyException(USER_ERROR_STUDY + " Study: " + studyId + ".", WebinCliException.ErrorType.USER_ERROR);
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    throw new StudyException(SYSTEM_ERROR_INTERNAL, WebinCliException.ErrorType.SYSTEM_ERROR);
                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    throw new StudyException(SYSTEM_ERROR_UNAVAILABLE, WebinCliException.ErrorType.SYSTEM_ERROR);
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new StudyException(WebinCli.INVALID_CREDENTIALS, WebinCliException.ErrorType.USER_ERROR);
                default:
                    throw new StudyException(SYSTEM_ERROR_OTHER, WebinCliException.ErrorType.SYSTEM_ERROR);
            }
        } catch (StudyException e) {
            throw e;
        } catch (Exception e) {
            throw new StudyException(SYSTEM_ERROR_OTHER, WebinCliException.ErrorType.SYSTEM_ERROR);
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

    private void extractResults(String result, String studyId) throws StudyException {
        try {
            JSONParser jsonParser = new JSONParser();
            StringReader reader = new StringReader(result);
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
            boolean canBeReferenced = (boolean)jsonObject.get("canBeReferenced");
            if (!canBeReferenced)
                throw new StudyException(USER_ERROR_STUDY + " Study: " + studyId + ".", WebinCliException.ErrorType.USER_ERROR);
            JSONArray jsonArray = (JSONArray)jsonObject.get("locusTags");
            projectId = (String) jsonObject.get("bioProjectId");
            if (jsonArray != null && !jsonArray.isEmpty())
                jsonArray.forEach(p -> locusTagsList.add( p.toString()));
        } catch (Exception e) {
            throw new StudyException(SYSTEM_ERROR_OTHER + e.getMessage(), WebinCliException.ErrorType.SYSTEM_ERROR);
        }
    }
}
