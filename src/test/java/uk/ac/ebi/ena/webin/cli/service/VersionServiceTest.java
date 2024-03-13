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

import org.junit.BeforeClass;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.entity.Version;
import uk.ac.ebi.ena.webin.cli.utils.RemoteServiceUrlHelper;

public class VersionServiceTest {

  private static final boolean TEST = true;

  private static Version validVersion;
  private static Version invalidVersion;

  @BeforeClass
  public static void setup() {
    VersionService versionService =
        new VersionService.Builder()
            .setWebinRestV1Uri(RemoteServiceUrlHelper.getWebinRestV1Url(TEST))
            .build();
    validVersion = versionService.getVersion("10.0.0");
    invalidVersion = versionService.getVersion("1.0.0");
  }

  @Test
  public void testValidVersion() {
    assertThat(validVersion.valid).isTrue();
    assertThat(validVersion.expire).isNotNull();
    assertThat(validVersion.update).isNotNull();
    assertThat(validVersion.minVersion).matches("\\d+\\.\\d+\\.\\d+");
    assertThat(validVersion.latestVersion).matches("\\d+\\.\\d+\\.\\d+");
  }

  @Test
  public void testInvalidVersion() {
    assertThat(invalidVersion.valid).isFalse();
    assertThat(invalidVersion.expire).isNotNull();
    assertThat(invalidVersion.update).isNotNull();
    assertThat(invalidVersion.minVersion).matches("\\d+\\.\\d+\\.\\d+");
    assertThat(invalidVersion.latestVersion).matches("\\d+\\.\\d+\\.\\d+");
  }
}
