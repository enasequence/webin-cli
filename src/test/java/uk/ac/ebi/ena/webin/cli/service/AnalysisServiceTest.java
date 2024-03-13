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
import uk.ac.ebi.ena.webin.cli.validator.reference.Analysis;

public class AnalysisServiceTest {

  private static final boolean TEST = true;

  @Test
  public void testGetAnalysisUsingPublicAnalysisId() {
    Analysis analysis = getAnalysisUsingValidId("ERZ000865");
    assertThat(analysis.getAnalysisId()).isEqualTo("ERZ000865");
  }

  @Test
  public void testGetAnalysisUsingPrivateAnalysisId() {
    Analysis analysis = getAnalysisUsingValidId("ERZ2661588");
    assertThat(analysis.getAnalysisId()).isEqualTo("ERZ2661588");
  }

  private Analysis getAnalysisUsingValidId(String id) {

    AnalysisService analysisService =
        new AnalysisService.Builder()
            .setWebinRestV1Uri(RemoteServiceUrlHelper.getWebinRestV1Url(TEST))
            .setUserName(WebinCliTestUtils.getTestWebinUsername())
            .setPassword(WebinCliTestUtils.getTestWebinPassword())
            .build();
    Analysis analysis = analysisService.getAnalysis(id);
    assertThat(analysis).isNotNull();
    assertThat(analysis.getName()).isNotNull();
    return analysis;
  }

  @Test
  public void testGetAnalysisUsingInvalidId() {
    String analysisId = "INVALID";
    AnalysisService analysisService =
        new AnalysisService.Builder()
            .setWebinRestV1Uri(RemoteServiceUrlHelper.getWebinRestV1Url(TEST))
            .setUserName(WebinCliTestUtils.getTestWebinUsername())
            .setPassword(WebinCliTestUtils.getTestWebinPassword())
            .build();

    assertThatThrownBy(() -> analysisService.getAnalysis(analysisId))
        .isInstanceOf(WebinCliException.class)
        .hasMessageContaining(WebinCliMessage.ANALYSIS_SERVICE_VALIDATION_ERROR.format(analysisId));
  }

  @Test
  public void testGetAnalysisUsingInvalidCredentials() {
    String analysisId = "INVALID";
    AnalysisService analysisService =
        new AnalysisService.Builder()
            .setWebinRestV1Uri(RemoteServiceUrlHelper.getWebinRestV1Url(TEST))
            .setUserName("INVALID")
            .setPassword("INVALID")
            .build();

    assertThatThrownBy(() -> analysisService.getAnalysis(analysisId))
        .isInstanceOf(WebinCliException.class)
        .hasMessageContaining(WebinCliMessage.CLI_AUTHENTICATION_ERROR.text());
  }
}
