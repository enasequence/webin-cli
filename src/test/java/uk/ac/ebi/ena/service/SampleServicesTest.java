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

import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.ena.WebinCliTestUtils;
import uk.ac.ebi.ena.entity.Sample;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class
SampleServicesTest {

    private static final boolean TEST = true;

    private static final String BIO_SAMPLE_ID = "SAMEA749881";
    private static final String SAMPLE_ID = "ERS000002";
    private static final String SCIENTIFIC_NAME = "Saccharomyces cerevisiae SK1";
    private static final String STRAIN_NAME = "SK1";
    private static final int TAX_ID = 580239;

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
        assertThatThrownBy(() -> {
            SampleService sampleService = new SampleService();
            sampleService.getSample(
                    id,
                    WebinCliTestUtils.getWebinUsername(),
                    WebinCliTestUtils.getWebinPassword(),
                    TEST);
        }).isInstanceOf(WebinCliException.class)
                .hasMessageContaining(SampleService.VALIDATION_ERROR + id);
    }

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
        assertThatThrownBy(() -> {
            SampleService sampleService = new SampleService();
            SampleService.getSourceFeature(
                    id,
                    WebinCliTestUtils.getWebinUsername(),
                    WebinCliTestUtils.getWebinPassword(),
                    TEST);
        }).isInstanceOf(WebinCliException.class)
                .hasMessageContaining(SampleService.VALIDATION_ERROR + id);

    }

    private void testGetSampleUsingValidId(String id) {
        SampleService sampleService = new SampleService();
        Sample sample = sampleService.getSample(
                id,
                WebinCliTestUtils.getWebinUsername(),
                WebinCliTestUtils.getWebinPassword(),
                TEST);
        assertThat(sample).isNotNull();
        assertThat(sample.getBiosampleId()).isEqualTo(BIO_SAMPLE_ID);
        assertThat(sample.getOrganism()).isEqualTo(SCIENTIFIC_NAME);
        assertThat(sample.getTaxId()).isEqualTo(TAX_ID);
    }

    private void testGetSourceFeatureUsingValidId(String id) {
        SourceFeature sourceFeature = SampleService.getSourceFeature(
                id,
                WebinCliTestUtils.getWebinUsername(),
                WebinCliTestUtils.getWebinPassword(),
                TEST);
        assertThat(sourceFeature).isNotNull();
        assertThat(sourceFeature.getSingleQualifier("db_xref")).isNotNull();
        assertThat(sourceFeature.getSingleQualifierValue("db_xref")).isEqualTo(String.valueOf(TAX_ID));
        assertThat(sourceFeature.getSingleQualifier("organism")).isNotNull();
        assertThat(sourceFeature.getSingleQualifierValue("organism")).isEqualTo(SCIENTIFIC_NAME);
        assertThat(sourceFeature.getSingleQualifier("strain")).isNotNull();
        assertThat(sourceFeature.getSingleQualifierValue("strain")).isEqualTo(STRAIN_NAME);
    }
}
