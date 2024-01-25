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
package uk.ac.ebi.ena.webin.cli.manifest.processor.metadata;

import static uk.ac.ebi.ena.webin.cli.manifest.processor.ProcessorTestUtils.createFieldValue;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.javafx.binding.StringFormatter;

import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.service.SampleService;
import uk.ac.ebi.ena.webin.cli.utils.ExceptionUtils;
import uk.ac.ebi.ena.webin.cli.utils.RemoteServiceUrlHelper;
import uk.ac.ebi.ena.webin.cli.utils.RetryUtils;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;
import uk.ac.ebi.ena.webin.cli.validator.reference.Sample;

public class
SampleProcessorTest {
    private final WebinCliParameters parameters = WebinCliTestUtils.getTestWebinCliParameters();
    private final Logger log = LoggerFactory.getLogger(SampleProcessorTest.class);
    private static final boolean TEST = true;

    @Test
    public void
    testCorrect() {
        SampleProcessor processor = new SampleProcessor(parameters,
                (Sample sample) -> Assert.assertEquals("SAMEA749881", sample.getBioSampleId()));

        ManifestFieldValue fieldValue = createFieldValue(ManifestFieldType.META, "SAMPLE", "ERS000002");
        ValidationResult result = new ValidationResult();
        processor.process(result, fieldValue);
        Assert.assertTrue(result.isValid());
        Assert.assertEquals("SAMEA749881", fieldValue.getValue());
    }


    @Test
    public void
    testIncorrect() {
        SampleProcessor processor = new SampleProcessor(parameters, Assert::assertNull);
        ManifestFieldValue fieldValue = createFieldValue(ManifestFieldType.META, "SAMPLE", "SRP000392");
        ValidationResult result = new ValidationResult();
        processor.process(result, fieldValue);
        Assert.assertFalse(result.isValid());
        Assert.assertEquals("SRP000392", fieldValue.getValue());
    }


    /**
     * Test Add and Modify with setSampleUpdate parameter.
     * The collectionDate in sample object will be updated to current date. 
     **/
    @Test
    public void
    testJsonSampleAddAndUpdate() throws JsonProcessingException {
        
        String sampleTestAlias = UUID.randomUUID().toString();
       
        // Create a new sample
        SampleProcessor processor = new SampleProcessor(parameters,
                (Sample sample) -> {
                    Assert.assertNotNull(sample.getBioSampleId());
                    Assert.assertEquals(getSampleCollectionDate(sample), "2010-01-20");
                });
        ManifestFieldValue fieldValue = createFieldValue(ManifestFieldType.META, "SAMPLE", getSampleJson(sampleTestAlias));
        ValidationResult result = new ValidationResult();
        processor.process(result, fieldValue);
        Assert.assertTrue(result.isValid());
        Assert.assertNotNull(fieldValue.getValue().startsWith("SAMEA"));
        String biosampleId = fieldValue.getValue();
        
        // Update sample
        JsonNode sampleJson = new ObjectMapper().readTree(getSampleJson(sampleTestAlias));
        // Update collection date to current date
        sampleJson.get("attributes").forEach(e ->{
            if(e.get("tag").asText().equals("collection date")){
                ((ObjectNode)e).put("value", LocalDate.now().toString());
            }
        });
        processor = new SampleProcessor(parameters,
                (Sample sample) -> {
                    // Updated collection date
                    Assert.assertEquals( getSampleCollectionDate(sample),(LocalDate.now().toString()));
                }
        );
        parameters.setSampleUpdate(true);

        Sample sampleObj = testGetSampleUsingValidId(sampleTestAlias);
        Assert.assertEquals(getSampleCollectionDate(sampleObj), "2010-01-20");
        fieldValue = createFieldValue(ManifestFieldType.META, "SAMPLE", sampleJson.toString());
        result = new ValidationResult();
        processor.process(result, fieldValue);
        Assert.assertTrue(result.isValid());
        Assert.assertEquals(biosampleId, fieldValue.getValue());
    }


    /**
     * Test Add and Modify without setSampleUpdate parameter.
     * The sample object must NOT be updated 
     **/
    @Test
    public void
    testJsonSampleAddAndNoSampleUpdate() throws JsonProcessingException {

        
        
        String sampleTestAlias = UUID.randomUUID().toString();

        // Create a new sample
        SampleProcessor processor = new SampleProcessor(parameters,
                (Sample sample) -> {
                    Assert.assertNotNull(sample.getBioSampleId());
                    Assert.assertEquals(getSampleCollectionDate(sample), "2010-01-20");
                });
        ManifestFieldValue fieldValue = createFieldValue(ManifestFieldType.META, "SAMPLE", getSampleJson(sampleTestAlias));
        ValidationResult result = new ValidationResult();
        processor.process(result, fieldValue);
        Assert.assertTrue(result.isValid());
        Assert.assertNotNull(fieldValue.getValue().startsWith("SAMEA"));
        String biosampleId = fieldValue.getValue();

        // Update sample
        JsonNode sampleJson = new ObjectMapper().readTree(getSampleJson(sampleTestAlias));
        // Update collection date to current date
        sampleJson.get("attributes").forEach(e ->{
            if(e.get("tag").asText().equals("collection date")){
                ((ObjectNode)e).put("value", LocalDate.now().toString());
            }
        });
        processor = new SampleProcessor(parameters,
                (Sample sample) -> {
                    // not updated
                    Assert.assertEquals( getSampleCollectionDate(sample),"2010-01-20");
                }
        );
        

        Sample sampleObj = testGetSampleUsingValidId(sampleTestAlias);
        Assert.assertEquals(getSampleCollectionDate(sampleObj), "2010-01-20");
        fieldValue = createFieldValue(ManifestFieldType.META, "SAMPLE", sampleJson.toString());
        result = new ValidationResult();
        processor.process(result, fieldValue);
        Assert.assertTrue(result.isValid());
        Assert.assertEquals(biosampleId, fieldValue.getValue());
    }

    private static String getSampleJson(String alias) {

        String sampleJson = "{" +
                "            \"alias\": \"%s\"," +
                "            \"title\": \"human gastric microbiota, mucosal\"," +
                "            \"organism\": {" +
                "                \"taxonId\": \"1284369\"" +
                "            }," +
                "            \"attributes\": [" +
                "                {" +
                "                    \"tag\": \"Geographic location (country and/or sea)\"," +
                "                    \"value\": \"France\"" +
                "                }," +
                "                {" +
                "                    \"tag\": \"collection date\"," +
                "                    \"value\": \"2010-01-20\"" +
                "                }," +
                "                {" +
                "                    \"tag\": \"ena-checklist\"," +
                "                    \"value\": \"ERC000011\"" +
                "                }" +
                "            ]" +
                "        }";

        return StringFormatter.format(sampleJson, alias).getValue();
    }

    private String getSampleCollectionDate(Sample sample){
        return sample.getAttributes().stream().filter(e-> e.getName().equals("collection date")).findFirst().get().getValue();
    }

    private Sample testGetSampleUsingValidId(String id) {
        SampleService sampleService = new SampleService.Builder()
                .setWebinRestV1Uri(RemoteServiceUrlHelper.getWebinRestV1Url(TEST))
                .setUserName( WebinCliTestUtils.getTestWebinUsername() )
                .setPassword( WebinCliTestUtils.getTestWebinPassword() )
                .setBiosamplesUri(RemoteServiceUrlHelper.getBiosamplesUrl(TEST))
                .setBiosamplesWebinUserName( WebinCliTestUtils.getTestWebinUsername() )
                .setBiosamplesWebinPassword( WebinCliTestUtils.getTestWebinPassword() )
                .build();
       
        return ExceptionUtils.executeWithRestExceptionHandling(

                () -> RetryUtils.executeWithRetry(
                        context -> sampleService.getSample( id ),
                        context -> log.warn("Retrying sending submission to server."),
                        HttpServerErrorException.class, ResourceAccessException.class
                ), WebinCliMessage.SERVICE_AUTHENTICATION_ERROR.format("Submit"),
                null,
                WebinCliMessage.SUBMIT_SAMPLE_SERVICE_SYSTEM_ERROR.text());
        
        
    }


}
