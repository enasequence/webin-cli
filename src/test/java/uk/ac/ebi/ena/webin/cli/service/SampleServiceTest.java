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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.web.client.HttpClientErrorException;

import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.validator.reference.Sample;

public class
SampleServiceTest {

    private static final boolean TEST = true;

    private static final String BIO_SAMPLE_ID = "SAMEA749881";
    private static final String SAMPLE_ID = "ERS000002";
    private static final String SCIENTIFIC_NAME = "Saccharomyces cerevisiae SK1";
    private static final int TAX_ID = 580239;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    
    @Test
    public void testGetSampleUsingPublicBioSampleId() {
        testGetSampleUsingValidId(BIO_SAMPLE_ID);
    }

    @Test
    public void testGetSampleUsingPublicSampleId() {
        testGetSampleUsingValidId(SAMPLE_ID);
    }

    @Test
    public void testGetSampleUsingInvalidId() {
        String id = "INVALID";
        exceptionRule.expect(HttpClientErrorException.NotFound.class);
        SampleService sampleService = new SampleService.Builder()
                .setUserName( "webin-256" )
                .setPassword( "sausages" )
                .setTest( TEST )
                .build();
        sampleService.getSample( id );
    }

    private void testGetSampleUsingValidId(String id) {
        SampleService sampleService = new SampleService.Builder()
                                                       .setUserName( WebinCliTestUtils.getTestWebinUsername() )
                                                       .setPassword( WebinCliTestUtils.getTestWebinPassword() )
                                                       .setTest( TEST )
                                                       .build();
        Sample sample = sampleService.getSample( id );
        assertThat(sample).isNotNull();
        assertThat(sample.getBioSampleId()).isEqualTo(BIO_SAMPLE_ID);
        assertThat(sample.getOrganism()).isEqualTo(SCIENTIFIC_NAME);
        assertThat(sample.getTaxId()).isEqualTo(TAX_ID);
    }
}
