package uk.ac.ebi.ena.service;

import lombok.Data;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ena.entity.Study;
import uk.ac.ebi.ena.service.handler.StudyServiceErrorHandler;
import uk.ac.ebi.ena.service.utils.HttpUtils;
import uk.ac.ebi.ena.webin.cli.WebinCliConfig;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

import java.util.List;

public class StudyService {

    @Data
    private static class StudyResponse {
        private String bioProjectId;
        private List<String> locusTags;
        private boolean canBeReferenced;
    }

    final static String VALIDATION_ERROR = "StudyServiceValidationError";
    final static String SYSTEM_ERROR = "StudyServiceSystemError";

    private WebinCliConfig config = new WebinCliConfig();

    private String getUri(boolean test) {
        String uri = "reference/project/{id}";
        return (test) ?
                config.getWebinRestUriTest() + uri :
                config.getWebinRestUriProd() + uri;
    }

    String getMessage(String messageKey, String studyId) {
        return config.getServiceMessage(messageKey) + " Study: " + studyId;
    }

    public Study
    getStudy(String studyId, String userName, String password, boolean test) {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setErrorHandler(new StudyServiceErrorHandler(
                getMessage(VALIDATION_ERROR, studyId),
                getMessage(SYSTEM_ERROR, studyId)));

        ResponseEntity<StudyResponse> response = restTemplate.exchange(
                getUri(test),
                HttpMethod.GET,
                new HttpEntity(HttpUtils.authHeader(userName, password)),
                StudyResponse.class,
                studyId.trim());

        StudyResponse studyResponse = response.getBody();
        if (studyResponse == null || !studyResponse.isCanBeReferenced()) {
            throw WebinCliException.createUserError(getMessage(VALIDATION_ERROR, studyId));
        }
        Study study = new Study();
        study.setProjectId(studyResponse.getBioProjectId());
        study.setLocusTags(studyResponse.getLocusTags());
        return study;
    }
}
