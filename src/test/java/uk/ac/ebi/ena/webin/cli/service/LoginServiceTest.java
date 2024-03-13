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

public class LoginServiceTest {

  @Test
  public void testValidLoginProd() {
    String userName =
        new LoginService(
                WebinCliTestUtils.getTestWebinUsername(),
                WebinCliTestUtils.getTestWebinPassword(),
                false)
            .login();

    assertThat(userName).matches("Webin-\\d+");
  }

  @Test
  public void testValidLoginTest() {
    String userName =
        new LoginService(
                WebinCliTestUtils.getTestWebinUsername(),
                WebinCliTestUtils.getTestWebinPassword(),
                true)
            .login();

    assertThat(userName).matches("Webin-\\d+");
  }

  @Test
  public void testInvalidLoginProd() {
    assertThatThrownBy(
            () ->
                new LoginService(WebinCliTestUtils.getTestWebinUsername(), "INVALID", false)
                    .login())
        .isInstanceOf(WebinCliException.class)
        .hasMessage(WebinCliMessage.CLI_AUTHENTICATION_ERROR.text());
  }

  @Test
  public void testInvalidLoginTest() {
    assertThatThrownBy(
            () ->
                new LoginService(WebinCliTestUtils.getTestWebinUsername(), "INVALID", true).login())
        .isInstanceOf(WebinCliException.class)
        .hasMessage(WebinCliMessage.CLI_AUTHENTICATION_ERROR.text());
  }
}
