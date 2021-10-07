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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;

public class
RatelimitServiceTest {
    @Test
    public void
    testRatelimitFalse() {
        assertThat(new RatelimitService.Builder()
                .setUserName(WebinCliTestUtils.getTestWebinUsername())
                .setPassword(WebinCliTestUtils.getTestWebinPassword())
                .setTest(true)
                .build()
                .ratelimit("GENOME", "Webin-1234", "PRJEB1234", "SAMP1234")
        ).isFalse();
    }
}
