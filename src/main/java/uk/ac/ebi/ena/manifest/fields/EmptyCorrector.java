package uk.ac.ebi.ena.manifest.fields;

import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.ena.manifest.ManifestFieldValue;

public class 
EmptyCorrector implements ManifestFieldCorrector 
{
    @Override public ValidationMessage<Origin> 
    correct( ManifestFieldValue field_value )
    {
        return null;
    }
}
