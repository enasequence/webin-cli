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

package uk.ac.ebi.ena.service;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class
VersionServiceTest {

    private static final boolean TEST = true;

    @Test
    public void testValidVersion() {
        VersionService versionService = new VersionService.Builder().setTest( TEST ).build();
        assertThat( versionService.isVersionValid( "3.0.0" ) ).isTrue();
    }

    @Test
    public void testInvalidVersion() {
        VersionService versionService = new VersionService.Builder().setTest( TEST ).build();
        assertThat( versionService.isVersionValid( "1.0.0" ) ).isFalse();
    }
}
