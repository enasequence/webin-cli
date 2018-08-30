package uk.ac.ebi.ena.manifest.fields;

import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.ena.manifest.ManifestFieldValue;

public interface
ManifestFieldValidator
{
    ValidationMessage<Origin> validate( ManifestFieldValue field_value );  
}
