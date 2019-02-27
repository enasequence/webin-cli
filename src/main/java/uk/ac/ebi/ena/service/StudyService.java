package uk.ac.ebi.ena.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ena.entity.Study;
import uk.ac.ebi.ena.service.handler.NotFoundErrorHandler;
import uk.ac.ebi.ena.service.utils.HttpHeaderBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;

import java.util.List;

public class StudyService extends AbstractService {

    protected 
    StudyService( AbstractBuilder<StudyService> builder )
    {
        super( builder );
    }

    
    public static class 
    Builder extends AbstractBuilder<StudyService>
    {
        @Override public StudyService
        build()
        {
            return new StudyService( this );
        }
    };
    

    private static class StudyResponse {
        public String bioProjectId;
        public List<String> locusTags;
        public boolean canBeReferenced;
    }

    public Study
    getStudy( String studyId )
    {
        return getStudy( studyId, getUserName(), getPassword(), getTest() );
    }
    

    private Study
    getStudy(String studyId, String userName, String password, boolean test) {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setErrorHandler(new NotFoundErrorHandler(
                WebinCliMessage.Service.STUDY_SERVICE_VALIDATION_ERROR.format(studyId),
                WebinCliMessage.Service.STUDY_SERVICE_SYSTEM_ERROR.format(studyId)));

        HttpHeaders headers = new HttpHeaderBuilder().basicAuth(userName, password).build();

        ResponseEntity<StudyResponse> response = restTemplate.exchange(
                getWebinRestUri("reference/project/{id}", test),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                StudyResponse.class,
                studyId.trim());

        StudyResponse studyResponse = response.getBody();
        if (studyResponse == null || !studyResponse.canBeReferenced) {
            throw WebinCliException.createUserError(
                    WebinCliMessage.Service.STUDY_SERVICE_VALIDATION_ERROR.format(studyId));
        }
        Study study = new Study();
        study.setProjectId(studyResponse.bioProjectId);
        study.setLocusTags(studyResponse.locusTags);
        return study;
    }
}
