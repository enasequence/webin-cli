package uk.ac.ebi.ena.manifest.fields;

import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.ena.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.rawreads.ControlledValueList;

public class 
CVFieldValidator implements ManifestFieldValidator
{
    ControlledValueList cv_list;
    
    public 
    CVFieldValidator( ControlledValueList cv_list )
    {
        this.cv_list = cv_list;
    }
    
    
    @Override public ValidationMessage<Origin> 
    validate( ManifestFieldValue field_value )
    {
        boolean contains = cv_list.contains( field_value.getValue() );
        return !contains ? ValidationMessage.error( "MANIFEST_INVALID_FIELD_VALUE", field_value.getName(), field_value.getValue(), cv_list.keyList() ) : null;
    }

}
