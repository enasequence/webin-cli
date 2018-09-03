package uk.ac.ebi.ena.manifest.fields;

import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.ena.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.rawreads.ControlledValueList;

public class 
CVKeyFieldCorrector implements ManifestFieldCorrector 
{
    ControlledValueList cv_list;
    
    public 
    CVKeyFieldCorrector( ControlledValueList cv_list )
    {
        this.cv_list = cv_list;
    }
    
    
    @Override public ValidationMessage<Origin>
    correct( ManifestFieldValue field_value )
    {
        String value = field_value.getValue();
        String corrected = cv_list.getKey( value );
        if( !value.equals( corrected ) )
        {
            field_value.setValue( cv_list.getKey( value ) );
            return ValidationMessage.info( "MANIFEST_FIELD_VALUE_WAS_CORRECTED", field_value.getName(), value, corrected );
        }
        
        return null;
    }
}
