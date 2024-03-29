/*
 * Copyright 2018-2023 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.utils.RemoteServiceUrlHelper;
import uk.ac.ebi.ena.webin.cli.validator.reference.Study;

public class StudyServiceTest {

  private static final boolean TEST = true;

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

    StudyService studyService =
        new StudyService.Builder()
            .setWebinRestV1Uri(RemoteServiceUrlHelper.getWebinRestV1Url(TEST))
            .setUserName(WebinCliTestUtils.getTestWebinUsername())
            .setPassword(WebinCliTestUtils.getTestWebinPassword())
            .build();
    Study study = studyService.getStudy(id);
    assertThat(study).isNotNull();
    assertThat(study.getBioProjectId()).isEqualTo(BIO_PROJECT_ID);
    assertThat(study.getLocusTags()).hasSize(1);
    assertThat(study.getLocusTags()).first().isEqualTo(LOCUS_TAG);
  }

  @Test
  public void testGetStudyUsingInvalidId() {
    String studyId = "INVALID";
    StudyService studyService =
        new StudyService.Builder()
            .setWebinRestV1Uri(RemoteServiceUrlHelper.getWebinRestV1Url(TEST))
            .setUserName(WebinCliTestUtils.getTestWebinUsername())
            .setPassword(WebinCliTestUtils.getTestWebinPassword())
            .build();
    assertThatThrownBy(() -> studyService.getStudy(studyId))
        .isInstanceOf(WebinCliException.class)
        .hasMessageContaining(WebinCliMessage.STUDY_SERVICE_VALIDATION_ERROR.format(studyId));
  }

  @Test
  public void testGetStudyUsingInvalidCredentials() {
    String studyId = "INVALID";
    StudyService studyService =
        new StudyService.Builder()
            .setWebinRestV1Uri(RemoteServiceUrlHelper.getWebinRestV1Url(TEST))
            .setUserName("INVALID")
            .setPassword("INVALID")
            .build();

    assertThatThrownBy(() -> studyService.getStudy(studyId))
        .isInstanceOf(WebinCliException.class)
        .hasMessageContaining(WebinCliMessage.CLI_AUTHENTICATION_ERROR.text());
  }
}
