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
package uk.ac.ebi.ena.webin.cli.manifest.processor;

import java.util.List;

import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestCVList;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationMessage;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;

public class
CVFieldProcessor implements ManifestFieldProcessor
{
    public final static CVFieldProcessor CV_BOOLEAN = new CVFieldProcessor(new ManifestCVList(
        "yes",
        "no",
        "true",
        "false",
        "Y",
        "N"));

    private final ManifestCVList cvList;

    public CVFieldProcessor(String ... values )
    {
        this.cvList = new ManifestCVList(values);
    }

    public CVFieldProcessor(ManifestCVList cvList )
    {
        this.cvList = cvList;
    }

    @Override public void
    process( ValidationResult result, ManifestFieldValue fieldValue )
    {
        String value = fieldValue.getValue();
        
        if( !cvList.contains( value ) ) {
            result.add(ValidationMessage.error( WebinCliMessage.CV_FIELD_PROCESSOR_ERROR, fieldValue.getName(), value, cvList.keyList() ) );
            return;
        }

        String corrected = cvList.getKey( value );

        if( !value.equals( corrected ) )
        {
            fieldValue.setValue( corrected );
            result.add(ValidationMessage.info( WebinCliMessage.CV_FIELD_PROCESSOR_FIELD_VALUE_CORRECTED, fieldValue.getName(), value, corrected ) );
        }
    }

    
    public List<String> 
    getValues() 
    {
        return cvList.keyList();
    }
}
