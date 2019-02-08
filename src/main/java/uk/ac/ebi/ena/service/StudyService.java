package uk.ac.ebi.ena.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ena.entity.Study;
import uk.ac.ebi.ena.service.handler.NotFoundErrorHandler;
import uk.ac.ebi.ena.service.utils.HttpHeaderBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliConfig;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

import java.util.List;

public class StudyService {

    private static class StudyResponse {
        private String bioProjectId;
        private List<String> locusTags;
        private boolean canBeReferenced;

        public String getBioProjectId() {
            return bioProjectId;
        }

        public void setBioProjectId(String bioProjectId) {
            this.bioProjectId = bioProjectId;
        }

        public List<String> getLocusTags() {
            return locusTags;
        }

        public void setLocusTags(List<String> locusTags) {
            this.locusTags = locusTags;
        }

        public boolean isCanBeReferenced() {
            return canBeReferenced;
        }

        public void setCanBeReferenced(boolean canBeReferenced) {
            this.canBeReferenced = canBeReferenced;
        }
    }

    final static String VALIDATION_ERROR = "StudyServiceValidationError";
    final static String SYSTEM_ERROR = "StudyServiceSystemError";

    String getMessage(String messageKey, String studyId) {
        return WebinCliConfig.getServiceMessage(messageKey) + " Study: " + studyId;
    }

    public Study
    getStudy(String studyId, String userName, String password, boolean test) {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setErrorHandler(new NotFoundErrorHandler(
                getMessage(VALIDATION_ERROR, studyId),
                getMessage(SYSTEM_ERROR, studyId)));

        ResponseEntity<StudyResponse> response = restTemplate.exchange(
                WebinCliConfig.getWebinRestUri("reference/project/{id}", test),
                HttpMethod.GET,
                new HttpEntity( (new HttpHeaderBuilder()).basicAuth(userName, password).get()),
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
