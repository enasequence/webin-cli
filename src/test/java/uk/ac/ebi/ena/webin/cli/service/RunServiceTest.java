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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.validator.reference.Run;

public class 
RunServiceTest 
{

    private final static boolean TEST = true;

    @Test public void 
    testGetRunUsingPublicRunId()
    {
        Run run = getRunUsingValidId( "ERR2486461" );
        assertThat( run.getRunId() ).isEqualTo( "ERR2486461" );
    }

    
    @Test public void 
    testGetRunUsingPrivateRunId()
    {
        Run run = getRunUsingValidId( "ERR6111314" );
        assertThat( run.getRunId() ).isEqualTo( "ERR6111314" );
    }

    
    private Run 
    getRunUsingValidId( String id )
    {

        RunService runService = new RunService.Builder()
                                              .setUserName( WebinCliTestUtils.getTestWebinUsername() )
                                              .setPassword( WebinCliTestUtils.getTestWebinPassword() )
                                              .setTest( TEST )
                                              .build();
        Run run = runService.getRun( id );
        assertThat( run ).isNotNull();
        assertThat( run.getName() ).isNotNull();
        return run;
    }

    
    @Test public void 
    testGetRunUsingInvalidId()
    {
        String runId = "INVALID";
        RunService runService = new RunService.Builder()
                                              .setUserName( WebinCliTestUtils.getTestWebinUsername() )
                                              .setPassword( WebinCliTestUtils.getTestWebinPassword() )
                                              .setTest( TEST )
                                              .build();

        assertThatThrownBy( () -> runService.getRun( runId ) ).isInstanceOf( WebinCliException.class )
                                                              .hasMessageContaining( WebinCliMessage.RUN_SERVICE_VALIDATION_ERROR.format( runId ) );
    }

    
    @Test public void 
    testGetRunUsingInvalidCredentials()
    {
        String runId = "INVALID";
        RunService runService = new RunService.Builder()
                                              .setUserName( "INVALID" )
                                              .setPassword( "INVALID" )
                                              .setTest( TEST )
                                              .build();

        assertThatThrownBy( () -> runService.getRun( runId ) ).isInstanceOf( WebinCliException.class )
                                                              .hasMessageContaining( WebinCliMessage.CLI_AUTHENTICATION_ERROR.text() );
    }
}
