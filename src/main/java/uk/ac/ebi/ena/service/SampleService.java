package uk.ac.ebi.ena.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.ena.entity.Sample;
import uk.ac.ebi.ena.service.handler.NotFoundErrorHandler;
import uk.ac.ebi.ena.service.utils.HttpHeaderBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class 
SampleService extends AbstractService 
{
    protected 
    SampleService( AbstractBuilder<SampleService> builder )
    {
        super( builder );
    }
    
    public static class 
    Builder extends AbstractBuilder<SampleService> 
    {
        @Override public SampleService
        build()
        {
            return new SampleService( this );
        }
    }


    private static class SampleResponse {
        public long taxId;
        public String organism;
        public String bioSampleId;
        public boolean canBeReferenced;
    }

    final static String VALIDATION_ERROR = "SampleServiceValidationError";
    final static String SYSTEM_ERROR = "SampleServiceSystemError";

    String getMessage(String messageKey, String sampleId) {
        return getServiceMessage(messageKey) + " Sample: " + sampleId;
    }

    
    public Sample 
    getSample( String sampleId )
    {
        return getSample( sampleId, getUserName(), getPassword(), getTest() );
    }

    
    private Sample getSample(String sampleId, String userName, String password, boolean test) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new NotFoundErrorHandler(
                getMessage(VALIDATION_ERROR, sampleId),
                getMessage(SYSTEM_ERROR, sampleId)));

        ResponseEntity<SampleResponse> response = restTemplate.exchange(
                getWebinRestUri("reference/sample/{id}", test),
                HttpMethod.GET,
                new HttpEntity<>((new HttpHeaderBuilder()).basicAuth(userName, password).build()),
                SampleResponse.class,
                sampleId.trim());

        SampleResponse sampleResponse = response.getBody();
        if (sampleResponse == null || !sampleResponse.canBeReferenced) {
            throw WebinCliException.createUserError(getMessage(VALIDATION_ERROR, sampleId));
        }
        Sample sample = new Sample();
        sample.setBiosampleId(sampleResponse.bioSampleId);
        sample.setTaxId(sampleResponse.taxId);
        sample.setOrganism(sampleResponse.organism);
        return sample;
    }
}
