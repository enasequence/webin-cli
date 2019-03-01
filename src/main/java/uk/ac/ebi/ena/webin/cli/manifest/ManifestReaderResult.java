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
package uk.ac.ebi.ena.webin.cli.manifest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import uk.ac.ebi.embl.api.validation.ValidationResult;

public class 
ManifestReaderResult 
{

    private final ValidationResult validationResult = new ValidationResult();
    private Collection<ManifestFieldValue> fields = new ArrayList<>();

    public ValidationResult 
    getValidationResult()
    {
        return validationResult;
    }

    
    public Collection<ManifestFieldValue> 
    getFields()
    {
        return fields;
    }

    
    public void 
    setFields( Collection<ManifestFieldValue> fields )
    {
        this.fields = fields;
    }


    public ManifestFieldValue
    getField( String fieldName ) 
    {
        try
        {
            return fields.stream()
                         .filter( field -> field.getName().equalsIgnoreCase( fieldName ) )
                         .findFirst()
                         .get();
        } catch( NoSuchElementException ex )
        {
            return null;
        }
    }


    public String 
    getValue( String fieldName ) 
    {
        try
        {
            return fields.stream()
                         .filter( field -> field.getName().equalsIgnoreCase( fieldName ) )
                         .findFirst()
                         .get()
                         .getValue();
        } catch( NoSuchElementException ex )
        {
            return null;
        }
    }

    
    public Collection<String> 
    getValues( String fieldName )
    {
        return fields.stream()
                     .filter( field -> field.getName().equalsIgnoreCase( fieldName ) )
                     .map( field -> field.getValue() )
                     .collect( Collectors.toList() );
    }

    public int 
    getCount( String fieldName )
    {
        return (int) fields.stream()
                           .filter( field -> field.getName().equalsIgnoreCase( fieldName ) )
                           .count();
    }
}
