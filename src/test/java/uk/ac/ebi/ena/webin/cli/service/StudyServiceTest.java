/*
 * Copyright 2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.webin.cli.service;

import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.entity.Study;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class
StudyServiceTest {

    private final static boolean TEST = true;

    private static final String BIO_PROJECT_ID = "PRJEB12332";
    private static final String STUDY_ID = "ERP013798";
    private static final String LOCUS_TAG = "BN3616";

    @Test
    public void testGetStudyUsingPublicBioProjectId() {
        testGetStudyUsingValidId(BIO_PROJECT_ID);
    }

    @Test
    public void testGetStudyUsingPublicStudyId() {
        testGetStudyUsingValidId(STUDY_ID);
    }
    
    private void testGetStudyUsingValidId(String id) {

        StudyService studyService = new StudyService.Builder()
                                                    .setUserName( WebinCliTestUtils.getWebinUsername() )
                                                    .setPassword( WebinCliTestUtils.getWebinPassword() )
                                                    .setTest( TEST )
                                                    .build();
        Study study = studyService.getStudy( id );
        assertThat(study).isNotNull();
        assertThat(study.getProjectId()).isEqualTo(BIO_PROJECT_ID);
        assertThat(study.getLocusTags()).hasSize(1);
        assertThat(study.getLocusTags()).first().isEqualTo(LOCUS_TAG);
    }

    @Test
    public void testGetStudyUsingInvalidId() {
        String studyId = "INVALID";
        StudyService studyService = new StudyService.Builder()
                                                    .setUserName( WebinCliTestUtils.getWebinUsername() )
                                                    .setPassword( WebinCliTestUtils.getWebinPassword() )
                                                    .setTest( TEST )
                                                    .build();
        assertThatThrownBy( () ->
                studyService.getStudy( studyId ) )
                .isInstanceOf(WebinCliException.class)
                .hasMessageContaining(WebinCliMessage.Service.STUDY_SERVICE_VALIDATION_ERROR.format(studyId));
    }

    @Test
    public void testGetStudyUsingInvalidCredentials() {
        String studyId = "INVALID";
        StudyService studyService = new StudyService.Builder()
                                                    .setUserName( "INVALID" )
                                                    .setPassword( "INVALID" )
                                                    .setTest( TEST )
                                                    .build();

        assertThatThrownBy(() ->
                studyService.getStudy( studyId )
        ).isInstanceOf(WebinCliException.class)
                .hasMessageContaining(WebinCliMessage.Cli.AUTHENTICATION_ERROR.format());
    }
}
