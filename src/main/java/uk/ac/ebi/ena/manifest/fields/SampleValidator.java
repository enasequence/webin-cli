package uk.ac.ebi.ena.manifest.fields;

import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.ena.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

public class 
SampleValidator implements ManifestFieldValidator 
{
    private final WebinCliParameters parameters;

    public 
    SampleValidator(WebinCliParameters parameters)
    {
        this.parameters = parameters;
    }

    
    @Override public ValidationMessage<Origin> 
    validate( ManifestFieldValue field_value )
    {
        String value = field_value.getValue();
        
        // TODO consider moving to ReadManifest*
        try
        {
            Sample.getSample( value, parameters.getUsername(), parameters.getPassword(), parameters.isTestMode());
            return null;
        } catch( WebinCliException e )
        {
            return ValidationMessage.error( "MANIFEST_SAMPLE_SERVER_ERROR", value, e.getMessage() );
        }
    }
}
