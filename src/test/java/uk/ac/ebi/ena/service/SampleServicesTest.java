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

import org.junit.Assert;
import org.junit.Test;

import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.ena.WebinCliTestUtils;
import uk.ac.ebi.ena.entity.Sample;

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
        testGetSample(BIO_SAMPLE_ID);
    }

    @Test
    public void testGetSampleUsingPublicSampleId() {
        testGetSample(SAMPLE_ID);
    }

    @Test
    public void testGetSourceFeatureUsingPublicBioSampleId() {
            testGetSourceFeature(BIO_SAMPLE_ID);
    }

    @Test
    public void testGetSourceFeatureUsingPublicSampleId() {
        testGetSourceFeature(SAMPLE_ID);
    }

    private void testGetSample(String id) {
        Sample sample = SampleService.getSample(
                id,
                WebinCliTestUtils.getWebinUsername(),
                WebinCliTestUtils.getWebinPassword(),
                TEST);
        Assert.assertNotNull(sample);
        Assert.assertEquals(BIO_SAMPLE_ID, sample.getBiosampleId());
        Assert.assertEquals(SCIENTIFIC_NAME, sample.getOrganism());
        Assert.assertEquals(TAX_ID, sample.getTaxId());
    }

    private void testGetSourceFeature(String id) {
        SourceFeature sourceFeature = SampleService.getSourceFeature(
                id,
                WebinCliTestUtils.getWebinUsername(),
                WebinCliTestUtils.getWebinPassword(),
                TEST);
        Assert.assertNotNull(sourceFeature);
        Assert.assertNotNull(sourceFeature.getSingleQualifier("db_xref"));
        Assert.assertEquals(String.valueOf(TAX_ID), sourceFeature.getSingleQualifierValue("db_xref"));
        Assert.assertNotNull(sourceFeature.getSingleQualifier("organism"));
        Assert.assertEquals(SCIENTIFIC_NAME, sourceFeature.getSingleQualifierValue("organism"));
        Assert.assertNotNull(sourceFeature.getSingleQualifier("strain"));
        Assert.assertEquals(STRAIN_NAME, sourceFeature.getSingleQualifierValue("strain"));
    }
}
