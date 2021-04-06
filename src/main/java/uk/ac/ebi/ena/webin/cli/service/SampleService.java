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

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.service.handler.NotFoundErrorHandler;
import uk.ac.ebi.ena.webin.cli.service.utils.HttpHeaderBuilder;
import uk.ac.ebi.ena.webin.cli.validator.reference.Sample;

public class 
SampleService extends WebinService
{
    protected 
    SampleService( AbstractBuilder<SampleService> builder )
    {
        super( builder );
    }

    public static class
    Builder extends AbstractBuilder<SampleService> {
        @Override
        public SampleService
        build() {
            return new SampleService(this);
        }
    }

    private static class SampleResponse {
        public int taxId;
        public String organism;
        public String bioSampleId;
        public boolean canBeReferenced;
    }

    public Sample
    getSample( String sampleId )
    {
        return getSample( sampleId, getUserName(), getPassword(), getTest() );
    }

    
    private Sample getSample(String sampleId, String userName, String password, boolean test) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new NotFoundErrorHandler(
                WebinCliMessage.SAMPLE_SERVICE_VALIDATION_ERROR.format(sampleId),
                WebinCliMessage.SAMPLE_SERVICE_SYSTEM_ERROR.format(sampleId)));

        ResponseEntity<SampleResponse> response = restTemplate.exchange(
                getWebinRestUri("cli/reference/sample/{id}", test),
                HttpMethod.GET,
                new HttpEntity<>((new HttpHeaderBuilder()).basicAuth(userName, password).build()),
                SampleResponse.class,
                sampleId.trim());

        SampleResponse sampleResponse = response.getBody();
        if (sampleResponse == null || !sampleResponse.canBeReferenced) {
            throw WebinCliException.userError(WebinCliMessage.SAMPLE_SERVICE_VALIDATION_ERROR.format(sampleId));
        }
        Sample sample = new Sample();
        sample.setBioSampleId(sampleResponse.bioSampleId);
        sample.setTaxId(sampleResponse.taxId);
        sample.setOrganism(sampleResponse.organism);
        return sample;
    }
}
