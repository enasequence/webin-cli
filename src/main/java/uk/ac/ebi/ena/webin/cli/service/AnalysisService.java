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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.entity.Analysis;
import uk.ac.ebi.ena.webin.cli.service.handler.NotFoundErrorHandler;
import uk.ac.ebi.ena.webin.cli.service.utils.HttpHeaderBuilder;

public class
AnalysisService extends AbstractService 
{

    protected 
    AnalysisService( AbstractBuilder<AnalysisService> builder )
    {
        super( builder );
    }

    
    public static class 
    Builder extends AbstractBuilder<AnalysisService> 
    {
        @Override public AnalysisService 
        build()
        {
            return new AnalysisService( this );
        }
    };

    
    public static class 
    AnalysisResponse 
    {
        public String id;
        public String alias;
        public boolean canBeReferenced;
    }

    
    public Analysis 
    getAnalysis( String analysisId )
    {
        return getAnalysis( analysisId, getUserName(), getPassword(), getTest() );
    }

    
    private Analysis 
    getAnalysis( String analysisId, String userName, String password, boolean test )
    {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler( new NotFoundErrorHandler( WebinCliMessage.Service.ANALYSIS_SERVICE_VALIDATION_ERROR.format( analysisId ),
                                                                WebinCliMessage.Service.ANALYSIS_SERVICE_SYSTEM_ERROR.format( analysisId ) ) );

        HttpHeaders headers = new HttpHeaderBuilder().basicAuth( userName, password ).build();

        ResponseEntity<AnalysisResponse> response = restTemplate.exchange( getWebinRestUri( "reference/analysis/{id}", test ),
                                                                           HttpMethod.GET,
                                                                           new HttpEntity<>( headers ),
                                                                           AnalysisResponse.class,
                                                                           analysisId.trim() );

        AnalysisResponse analysisResponse = response.getBody();
        if( analysisResponse == null || !analysisResponse.canBeReferenced )
            throw WebinCliException.userError( WebinCliMessage.Service.ANALYSIS_SERVICE_VALIDATION_ERROR.format( analysisId ) );

        return new Analysis( analysisResponse );
    }
}
