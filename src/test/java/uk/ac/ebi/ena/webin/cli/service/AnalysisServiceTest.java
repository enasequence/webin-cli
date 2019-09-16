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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.validator.reference.Analysis;

public class 
AnalysisServiceTest 
{

    private final static boolean TEST = true;

    @Test public void 
    testGetAnalysisUsingPublicAnalysisId()
    {
        Analysis analysis = getAnalysisUsingValidId( "ERZ000865" );
        assertThat( analysis.getAnalysisId() ).isEqualTo( "ERZ000865" );
    }

    
    @Test public void 
    testGetAnalysisUsingPrivateAnalysisId()
    {
        assertThatThrownBy( () -> getAnalysisUsingValidId( "ERZ000883" ) ).isInstanceOf( WebinCliException.class )
                                                                          .hasMessageContaining( WebinCliMessage.Service.ANALYSIS_SERVICE_VALIDATION_ERROR.format( "ERZ000883" ) );
    }

    
    private Analysis 
    getAnalysisUsingValidId( String id )
    {

        AnalysisService analysisService = new AnalysisService.Builder()
                                                             .setUserName( WebinCliTestUtils.getTestWebinUsername() )
                                                             .setPassword( WebinCliTestUtils.getTestWebinPassword() )
                                                             .setTest( TEST )
                                                             .build();
        Analysis analysis = analysisService.getAnalysis( id );
        assertThat( analysis ).isNotNull();
        assertThat( analysis.getName() ).isNotNull();
        return analysis;
    }

    
    @Test public void 
    testGetAnalysisUsingInvalidId()
    {
        String analysisId = "INVALID";
        AnalysisService analysisService = new AnalysisService.Builder()
                                                             .setUserName( WebinCliTestUtils.getTestWebinUsername() )
                                                             .setPassword( WebinCliTestUtils.getTestWebinPassword() )
                                                             .setTest( TEST )
                                                             .build();

        assertThatThrownBy( () -> analysisService.getAnalysis( analysisId ) ).isInstanceOf( WebinCliException.class )
                                                                             .hasMessageContaining( WebinCliMessage.Service.ANALYSIS_SERVICE_VALIDATION_ERROR.format( analysisId ) );
    }

    
    @Test public void 
    testGetAnalysisUsingInvalidCredentials()
    {
        String analysisId = "INVALID";
        AnalysisService analysisService = new AnalysisService.Builder()
                                                             .setUserName( "INVALID" )
                                                             .setPassword( "INVALID" )
                                                             .setTest( TEST )
                                                             .build();

        assertThatThrownBy( () -> analysisService.getAnalysis( analysisId ) ).isInstanceOf( WebinCliException.class )
                                                                             .hasMessageContaining( WebinCliMessage.Cli.AUTHENTICATION_ERROR.format() );
    }
}
