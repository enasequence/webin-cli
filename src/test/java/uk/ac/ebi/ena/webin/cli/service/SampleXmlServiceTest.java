/*
 * Copyright 2018-2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.validator.reference.Attribute;
import uk.ac.ebi.ena.webin.cli.validator.reference.Sample;

public class
SampleXmlServiceTest {

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
        SampleXmlService sampleService = new SampleXmlService.Builder()
                                                                     .setUserName( WebinCliTestUtils.getTestWebinUsername() )
                                                                     .setPassword( WebinCliTestUtils.getTestWebinPassword() )
                                                                     .setTest( TEST )
                                                                     .build();
        assertThatThrownBy(() -> {
            sampleService.getSample( id );
        }).isInstanceOf(WebinCliException.class)
                .hasMessageContaining(WebinCliMessage.SAMPLE_SERVICE_VALIDATION_ERROR.format(id));
    }

    private void testGetSourceFeatureUsingValidId(String id) {
        SampleXmlService sampleService = new SampleXmlService.Builder()
                                                                     .setUserName( WebinCliTestUtils.getTestWebinUsername() )
                                                                     .setPassword( WebinCliTestUtils.getTestWebinPassword() )
                                                                     .setTest( TEST )
                                                                     .build();
        Sample sample = sampleService.getSample( id );
        assertThat(sample).isNotNull();
        assertThat(sample.getTaxId()).isEqualTo(TAX_ID);
        assertThat(sample.getOrganism()).isEqualTo(SCIENTIFIC_NAME);
        boolean assertStrain = false;
        for (Attribute attribute : sample.getAttributes()) {
            if (attribute.getName().equals("strain") && attribute.getValue().equals(STRAIN_NAME)) {
                assertStrain = true;
            }
        }
        assertThat(assertStrain);
    }
}
