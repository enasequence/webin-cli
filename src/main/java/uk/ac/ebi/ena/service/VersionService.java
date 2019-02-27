/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.service;

import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.ena.entity.Version;
import uk.ac.ebi.ena.service.handler.DefaultErrorHander;

public class 
VersionService extends AbstractService
{
    public static class 
    Builder extends AbstractBuilder<VersionService> {
        public VersionService
        build()
        {
          return new VersionService( this );
        }
    }

    protected 
    VersionService( Builder builder )
    {
        super( builder );
    }

    private final static String SYSTEM_ERROR = "VersionServiceSystemError";

    public Version getVersion(String version )
    {
        return getVersion( version, getTest() );
    }

    private Version getVersion(String version, boolean test ) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultErrorHander(getServiceMessage(SYSTEM_ERROR)));
        return restTemplate.getForObject(
                getWebinRestUri("/cli/{version}", test), Version.class, version);
    }
}
