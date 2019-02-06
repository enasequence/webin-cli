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

import uk.ac.ebi.ena.WebinCliTestUtils;
import uk.ac.ebi.ena.entity.Study;

public class
StudyServicesTest {

    private final static boolean TEST = true;

    private static final String BIO_PROJECT_ID = "PRJEB12332";
    private static final String STUDY_ID = "ERP013798";
    private static final String LOCUS_TAG = "BN3616";

    @Test
    public void testGetStudyUsingPublicBioProjectId() {
        testGetStudy(BIO_PROJECT_ID);
    }

    @Test
    public void testGetStudyUsingPublicStudyId() {
        testGetStudy(STUDY_ID);
    }
    
    private void testGetStudy(String id) {
        Study study = StudyService.getStudy(
                id,
                WebinCliTestUtils.getWebinUsername(),
                WebinCliTestUtils.getWebinPassword(),
                TEST);
        Assert.assertNotNull(study);
        Assert.assertEquals(BIO_PROJECT_ID, study.getProjectId());
        Assert.assertEquals(1, study.getLocusTagsList().size());
        Assert.assertEquals(LOCUS_TAG, study.getLocusTagsList().get(0));
    }
}
