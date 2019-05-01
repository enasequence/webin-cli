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
package uk.ac.ebi.ena.webin.cli.entity;

import uk.ac.ebi.ena.webin.cli.service.AnalysisService;

public class 
Analysis 
{
    private String analysis_id;
    private String alias;

    public 
    Analysis( AnalysisService.AnalysisResponse analysis_response )
    {
        this( analysis_response.id, analysis_response.alias );
    }


    public 
    Analysis( String analysis_id, String alias )
    {
        this.analysis_id = analysis_id;
        this.alias       = alias;
    }
    
    
    public String 
    getAnalysisId()
    {
        return analysis_id;
    }

    
    public String 
    getAlias()
    {
        return alias;
    }
}
