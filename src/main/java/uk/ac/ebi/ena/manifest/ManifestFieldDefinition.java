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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import uk.ac.ebi.ena.manifest.fields.ManifestFieldCorrector;
import uk.ac.ebi.ena.manifest.fields.ManifestFieldValidator;
import uk.ac.ebi.ena.manifest.fields.EmptyCorrector;

public class 
ManifestFieldDefinition 
{
    private final String name;
    private final ManifestFieldType type;
    private final int minCount;
    private final int maxCount;
    private final List<String> fieldValueOrFileSuffix;
    private final List<ManifestFieldValidator> validators;
    private final ManifestFieldCorrector corrector;

    
    public List<ManifestFieldValidator>
    getFieldValidators()
    {
        return validators;
    }
    
    
    public ManifestFieldCorrector
    getFieldCorrector()
    {
        return corrector;
    }

    
    public 
    ManifestFieldDefinition( String name, ManifestFieldType type, int minCount, int maxCount )
    {
        this.name = name;
        this.type = type;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.fieldValueOrFileSuffix = Collections.emptyList();
        this.validators = Collections.emptyList();
        this.corrector  = new EmptyCorrector();
    }

    
    public 
    ManifestFieldDefinition( String name, ManifestFieldType type, int minCount, int maxCount, List<String> fieldValueOrFileSuffix )
    {
        this.name = name;
        this.type = type;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.fieldValueOrFileSuffix = fieldValueOrFileSuffix;
        this.validators = Collections.emptyList();
        this.corrector  = new EmptyCorrector();
    }


    public 
    ManifestFieldDefinition( String name, ManifestFieldType type, int minCount, int maxCount, ManifestFieldCorrector corrector, ManifestFieldValidator... validators )
    {
        this.name = name;
        this.type = type;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.fieldValueOrFileSuffix = Collections.emptyList();
        this.validators = Arrays.asList( validators );
        this.corrector  = corrector;
    }
    
    
    public 
    ManifestFieldDefinition( String name, ManifestFieldType type, int minCount, int maxCount, ManifestFieldValidator... validators )
    {
        this( name, type, minCount, maxCount, new EmptyCorrector(), validators );
    }
    
    
    
    public String 
    getName()
    {
        return name;
    }

    
    public ManifestFieldType 
    getType()
    {
        return type;
    }

    
    public int 
    getMinCount()
    {
        return minCount;
    }

    
    public int 
    getMaxCount()
    {
        return maxCount;
    }

    
    public boolean 
    isFieldValueOrFileSuffix()
    {
        return fieldValueOrFileSuffix != null && !fieldValueOrFileSuffix.isEmpty();
    }

    
    public List<String> 
    getFieldValueOrFileSuffix()
    {
        return fieldValueOrFileSuffix;
    }
    
    
    public String
    toString()
    {
        return String.format( "%s<%s>[%d, %d]{%s}", name, type, minCount, maxCount, fieldValueOrFileSuffix );
    }
}
