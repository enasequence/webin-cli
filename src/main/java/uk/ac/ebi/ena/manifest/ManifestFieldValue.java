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

package uk.ac.ebi.ena.manifest;

import uk.ac.ebi.embl.api.validation.Origin;

public class 
ManifestFieldValue 
{
    private ManifestFieldDefinition definition;
    private String value;
    private Origin origin;
    private boolean validFieldValueOrFileSuffix = true;

    public 
    ManifestFieldValue( ManifestFieldDefinition definition, String value, Origin origin )
    {
        assert( definition != null );
        assert( value != null );
        this.definition = definition;
        this.value = value;
        this.origin = origin;
    }
    
    public String 
    getName() 
    {
        return definition.getName();
    }

    
    public ManifestFieldDefinition 
    getDefinition() 
    {
        return definition;
    }

    
    public String 
    getValue() 
    {
        return value;
    }

    
    public void 
    setValue( String value )
    {
        this.value = value;
    }

    
    public boolean 
    isValidFieldValueOrFileSuffix() 
    {
        return validFieldValueOrFileSuffix;
    }

    
    public void 
    setValidFieldValueOrFileSuffix( boolean validFieldValueOrFileSuffix )
    {
        this.validFieldValueOrFileSuffix = validFieldValueOrFileSuffix;
    }

    
    public Origin 
    getOrigin() 
    {
        return origin;
    }
    
    
    public String
    toString()
    {
        return String.format( "%s = %s [%s], %s", definition, value, null != origin ? origin.getOriginText() : "", validFieldValueOrFileSuffix );
    }
}
