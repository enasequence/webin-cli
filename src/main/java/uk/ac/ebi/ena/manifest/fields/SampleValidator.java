package uk.ac.ebi.ena.manifest.fields;

import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.ena.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class 
SampleValidator implements ManifestFieldValidator 
{
    private String userName;
    private String passwd;
    private boolean test_mode;
    
    public 
    SampleValidator( String userName, String passwd, boolean test_mode )
    {
        this.userName = userName;
        this.passwd   = passwd;
    }
    
    
    @Override public ValidationMessage<Origin> 
    validate( ManifestFieldValue field_value )
    {
        String value = field_value.getValue();
        
        // TODO consider moving to ReadManifest*
        try
        {
            Sample.getSample( value, userName, passwd, test_mode );
            return null;
        } catch( WebinCliException e )
        {
            return ValidationMessage.error( "MANIFEST_SAMPLE_SERVER_ERROR", value, e.getMessage() );
        }
    }

}
