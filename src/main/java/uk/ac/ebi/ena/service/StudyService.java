package uk.ac.ebi.ena.service;

import lombok.Data;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ena.entity.Study;
import uk.ac.ebi.ena.service.handler.StudyServiceErrorHandler;
import uk.ac.ebi.ena.service.utils.HttpUtils;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

import java.util.List;

public class StudyService {

    @Data
    private static class StudyResponse {
        private String bioProjectId;
        private List<String> locusTags;
        private boolean canBeReferenced;
    }

    public final static String VALIDATION_ERROR =
            "Unknown study or the study cannot be referenced by your submission account. " +
            "Studies must be submitted before they can be referenced in the submission. Study: ";

    public final static String SYSTEM_ERROR =
            "A server error occurred when retrieving study information. Study: ";

    private static String getUri(boolean test) {
        return (test) ?
                "https://wwwdev.ebi.ac.uk/ena/submit/drop-box/reference/project/{id}" :
                "https://www.ebi.ac.uk/ena/submit/drop-box/reference/project/{id}";
    }

    public Study
    getStudy(String studyId, String userName, String password, boolean test) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new StudyServiceErrorHandler(studyId, VALIDATION_ERROR, SYSTEM_ERROR));

        ResponseEntity<StudyResponse> response = restTemplate.exchange(
                getUri(test),
                HttpMethod.GET,
                new HttpEntity(HttpUtils.authHeader(userName, password)),
                StudyResponse.class,
                studyId.trim());

        StudyResponse studyResponse = response.getBody();
        if (studyResponse == null || !studyResponse.isCanBeReferenced()) {
            throw WebinCliException.createUserError(VALIDATION_ERROR, studyId);
        }
        Study study = new Study();
        study.setProjectId(studyResponse.getBioProjectId());
        study.setLocusTags(studyResponse.getLocusTags());
        return study;
    }
}
