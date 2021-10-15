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
package uk.ac.ebi.ena.webin.cli;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.ena.webin.cli.manifest.processor.metadata.*;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;
import uk.ac.ebi.ena.webin.cli.validator.reference.Sample;
import uk.ac.ebi.ena.webin.cli.validator.reference.Study;

public class WebinCliExecutorBuilder<M extends Manifest, R extends ValidationResponse> {

    private final static String AUTH_JSON="{\"authRealms\":[\"ENA\"],\"password\":\"sausages\",\"username\":\"Webin-256\"}";
    private final static String TEST_AUTH_URL="https://wwwdev.ebi.ac.uk/ena/submit/webin/auth/token";
    private static String token = "";
    private final Class<M> manifestClass;
    private final WebinCliParameters parameters = WebinCliTestUtils.getTestWebinCliParameters();

    private SampleProcessor sampleProcessor;
    private StudyProcessor studyProcessor;
    private SampleXmlProcessor sampleXmlProcessor;
    private RunProcessor runProcessor;
    private AnalysisProcessor analysisProcessor;
    private String webinAuthToken;

    public enum MetadataProcessorType {
        DEFAULT,
        MOCK
    }

    public WebinCliExecutorBuilder(Class<M> manifestClass, MetadataProcessorType metadataProcessorType) {
        this.manifestClass = manifestClass;
        if (MetadataProcessorType.MOCK.equals(metadataProcessorType)) {
            this.sampleProcessor = mock(SampleProcessor.class);
            this.studyProcessor = mock(StudyProcessor.class);
            this.sampleXmlProcessor = mock(SampleXmlProcessor.class);
            this.runProcessor = mock(RunProcessor.class);
            this.analysisProcessor = mock(AnalysisProcessor.class);

            doNothing().when(this.sampleProcessor).process(any(), any());
            doNothing().when(this.studyProcessor).process(any(), any());
            doNothing().when(this.sampleXmlProcessor).process(any(), any());
            doNothing().when(this.runProcessor).process(any(), any());
            doNothing().when(this.analysisProcessor).process(any(), any());
        }
    }

    public WebinCliExecutorBuilder study(Study study) {
        this.studyProcessor = spy(new StudyProcessor(null));
        doAnswer((InvocationOnMock invocation) ->
                {
                    StudyProcessor processor = (StudyProcessor)invocation.getMock();
                    processor.getCallback().notify(study);
                    return null;
                }
        ).when(this.studyProcessor).process(any(), any());
        return this;
    }

    public WebinCliExecutorBuilder sample(Sample sample) {
        this.sampleProcessor = spy(new SampleProcessor(null));
        doAnswer((InvocationOnMock invocation) ->
                {
                    SampleProcessor processor = (SampleProcessor)invocation.getMock();
                    processor.getCallback().notify(sample);
                    return null;
                }
        ).when(this.sampleProcessor).process(any(), any());
        return this;
    }

    public WebinCliExecutor<M, R> build(File manifestFile, File inputDir) {
        parameters.setManifestFile(manifestFile);
        parameters.setInputDir(inputDir);
        parameters.setOutputDir(WebinCliTestUtils.createTempDir());
        parameters.setSampleProcessor(sampleProcessor);
        parameters.setStudyProcessor(studyProcessor);
        parameters.setSampleXmlProcessor(sampleXmlProcessor);
        parameters.setRunProcessor(runProcessor);
        parameters.setAnalysisProcessor(analysisProcessor);
        parameters.setWebinAuthToken(getAuthToken());
        return WebinCliContext.createExecutor(manifestClass, parameters);
    }

    public WebinCliParameters getParameters() {
        return parameters;
    }

    private String getAuthToken(){
        if(StringUtils.isNotEmpty(token)){
            return token;
        }
        RestTemplate restTemplate=new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request =
                new HttpEntity<String>(AUTH_JSON, headers);
        ResponseEntity<String> response =
                restTemplate.postForEntity(TEST_AUTH_URL,request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        token = response.getBody();
        return token;
    }
}
