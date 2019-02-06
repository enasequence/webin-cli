package uk.ac.ebi.ena.service;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import uk.ac.ebi.ena.entity.Study;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class StudyService {

   private StudyService() {
        throw new UnsupportedOperationException();
    }

    private final static String VALIDATION_ERROR_STUDY = "Unknown study (project) or the study cannot be referenced by your submission account. " +
            "Studies must be submitted before they can be referenced in the submission. Study: ";
    private final static String SYSTEM_ERROR_INTERNAL = "An internal server error occurred when retrieving study information. ";
    private final static String SYSTEM_ERROR_UNAVAILABLE = "A service unavailable error occurred when retrieving study information. ";
    private final static String SYSTEM_ERROR_OTHER = "A server error occurred when retrieving study information. ";

    public static Study
    getStudy( String studyId, String userName, String password, boolean TEST )
    {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet( ( TEST ? "https://wwwdev.ebi.ac.uk/ena/submit/drop-box/reference/project/" : "https://www.ebi.ac.uk/ena/submit/drop-box/reference/project/" ) +  URLEncoder.encode( studyId.trim(), "UTF-8" ) );
            String encoding = Base64.getEncoder().encodeToString((userName + ":" + password).getBytes());
            httpGet.setHeader("Authorization", "Basic " + encoding);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            int responsecode = response.getStatusLine().getStatusCode();
            switch (responsecode) {
                case HttpStatus.SC_OK:
                    List<String> resultsList = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
                    String result = resultsList.stream()
                            .collect(Collectors.joining(" "));
                    return createStudy( result, studyId );
                case HttpStatus.SC_BAD_REQUEST:
                case HttpStatus.SC_NOT_FOUND:
                    throw WebinCliException.createValidationError(VALIDATION_ERROR_STUDY, studyId);
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

    @SuppressWarnings( "unchecked" )
    private static Study createStudy(String result, String studyId) {
        Study study = new Study();
        try {
            JSONParser jsonParser = new JSONParser();
            StringReader reader = new StringReader(result);
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
            boolean canBeReferenced = (boolean)jsonObject.get("canBeReferenced");
            if (!canBeReferenced)
                throw WebinCliException.createUserError(VALIDATION_ERROR_STUDY, studyId);
            JSONArray jsonArray = (JSONArray)jsonObject.get("locusTags");
            study.setProjectId((String) jsonObject.get("bioProjectId"));
            List<String> locusTagsList = new ArrayList<>();
            if (jsonArray != null && !jsonArray.isEmpty()) {
                jsonArray.forEach(p -> locusTagsList.add(p.toString()));
            }
            study.setLocusTagsList(locusTagsList);
            return study;
        } catch (IOException | ParseException e) {
            throw WebinCliException.createSystemError(SYSTEM_ERROR_OTHER, e.getMessage());
        }
    }

}
