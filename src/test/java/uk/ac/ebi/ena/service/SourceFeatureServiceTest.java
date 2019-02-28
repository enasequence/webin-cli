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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.Test;

import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.ena.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;

public class
SourceFeatureServiceTest {

    private static final boolean TEST = true;

    private static final String BIO_SAMPLE_ID = "SAMEA749881";
    private static final String SAMPLE_ID = "ERS000002";
    private static final String SCIENTIFIC_NAME = "Saccharomyces cerevisiae SK1";
    private static final String STRAIN_NAME = "SK1";
    private static final int TAX_ID = 580239;

    @Test
    public void testGetSourceFeatureUsingPublicBioSampleId() {
            testGetSourceFeatureUsingValidId(BIO_SAMPLE_ID);
    }

    @Test
    public void testGetSourceFeatureUsingPublicSampleId() {
        testGetSourceFeatureUsingValidId(SAMPLE_ID);
    }

    @Test
    public void testGetSourceFeatureUsingInvalidId() {
        String id = "INVALID";
        SourceFeatureService sampleService = new SourceFeatureService.Builder()
                                                                     .setUserName( WebinCliTestUtils.getWebinUsername() )
                                                                     .setPassword( WebinCliTestUtils.getWebinPassword() )
                                                                     .setTest( TEST )
                                                                     .build();
        assertThatThrownBy(() -> {
            sampleService.getSourceFeature( id );
        }).isInstanceOf(WebinCliException.class)
                .hasMessageContaining(WebinCliMessage.Service.SAMPLE_SERVICE_VALIDATION_ERROR.format(id));
    }

    private void testGetSourceFeatureUsingValidId(String id) {
        SourceFeatureService sampleService = new SourceFeatureService.Builder()
                                                                     .setUserName( WebinCliTestUtils.getWebinUsername() )
                                                                     .setPassword( WebinCliTestUtils.getWebinPassword() )
                                                                     .setTest( TEST )
                                                                     .build();
        SourceFeature sourceFeature = sampleService.getSourceFeature( id );
        assertThat(sourceFeature).isNotNull();
        assertThat(sourceFeature.getSingleQualifier("db_xref")).isNotNull();
        assertThat(sourceFeature.getSingleQualifierValue("db_xref")).isEqualTo(String.valueOf(TAX_ID));
        assertThat(sourceFeature.getSingleQualifier("organism")).isNotNull();
        assertThat(sourceFeature.getSingleQualifierValue("organism")).isEqualTo(SCIENTIFIC_NAME);
        assertThat(sourceFeature.getSingleQualifier("strain")).isNotNull();
        assertThat(sourceFeature.getSingleQualifierValue("strain")).isEqualTo(STRAIN_NAME);
    }
}
