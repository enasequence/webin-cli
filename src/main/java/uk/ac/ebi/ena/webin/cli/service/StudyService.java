/*
 * Copyright 2018-2021 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.service;

import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.service.handler.NotFoundErrorHandler;
import uk.ac.ebi.ena.webin.cli.service.utils.HttpHeaderBuilder;
import uk.ac.ebi.ena.webin.cli.validator.reference.Study;

public class StudyService extends WebinService {

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
                WebinCliMessage.STUDY_SERVICE_VALIDATION_ERROR.format(studyId),
                WebinCliMessage.STUDY_SERVICE_SYSTEM_ERROR.format(studyId)));

        HttpHeaders headers = new HttpHeaderBuilder().basicAuth(userName, password).build();

        ResponseEntity<StudyResponse> response = restTemplate.exchange(
                getWebinRestUri("cli/reference/project/{id}", test),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                StudyResponse.class,
                studyId.trim());

        StudyResponse studyResponse = response.getBody();
        if (studyResponse == null || !studyResponse.canBeReferenced) {
            throw WebinCliException.userError(
                    WebinCliMessage.STUDY_SERVICE_VALIDATION_ERROR.format(studyId));
        }
        Study study = new Study();
        study.setStudyId(studyId);
        study.setBioProjectId(studyResponse.bioProjectId);
        study.setLocusTags(studyResponse.locusTags);
        return study;
    }
}
