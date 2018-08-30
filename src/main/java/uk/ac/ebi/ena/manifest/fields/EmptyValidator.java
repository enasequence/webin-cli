package uk.ac.ebi.ena.manifest.fields;

import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.ena.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.rawreads.ControlledValueList;

public class 
EmptyValidator implements ManifestFieldValidator
{
    ControlledValueList cv_list;
   
    @Override public ValidationMessage<Origin> 
    validate( ManifestFieldValue field_value )
    {
        return null;
    }
}
