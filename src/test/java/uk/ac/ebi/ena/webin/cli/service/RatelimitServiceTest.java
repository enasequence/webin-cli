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

import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.utils.RemoteServiceUrlHelper;

public class
RatelimitServiceTest {
    @Test
    public void
    testRatelimitFalse() {
        assertThat(new RatelimitService.Builder()
            .setWebinRestV1Uri(RemoteServiceUrlHelper.getWebinRestV1Url(true))
            .setUserName(WebinCliTestUtils.getTestWebinUsername())
            .setPassword(WebinCliTestUtils.getTestWebinPassword())
            .build()
            .ratelimit("GENOME", "Webin-1234", "ERP129098", "ERS11253593")
            .isRateLimited()
        ).isFalse();
    }

    @Test
    public void
    testRatelimitTrueFailsWithBioSampleAccession() {
        assertThat(new RatelimitService.Builder()
                .setWebinRestV1Uri(RemoteServiceUrlHelper.getWebinRestV1Url(true))
                .setUserName(WebinCliTestUtils.getTestWebinUsername())
                .setPassword(WebinCliTestUtils.getTestWebinPassword())
                .build()
                .ratelimit("genome", "Webin-58771", "ERP129098", "SAMEA113618321")
                .isRateLimited()
        ).isTrue();
    }

    @Test
    public void
    testRatelimitTrueFailsWithBioProjectAccession() {
        assertThat(new RatelimitService.Builder()
                .setWebinRestV1Uri(RemoteServiceUrlHelper.getWebinRestV1Url(true))
                .setUserName(WebinCliTestUtils.getTestWebinUsername())
                .setPassword(WebinCliTestUtils.getTestWebinPassword())
                .build()
                .ratelimit("genome", "Webin-58771", "PRJEB44987", "ERS15612497")
                .isRateLimited()
        ).isTrue();
    }
}
