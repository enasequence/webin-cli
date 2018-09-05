package uk.ac.ebi.ena.manifest.fields;

import java.util.regex.Pattern;

import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.ena.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

public class 
StudyValidator implements ManifestFieldValidator 
{
    private final static Pattern p = Pattern.compile( "[EDS]RP[0-9]{6,12}", Pattern.CASE_INSENSITIVE );

    private final WebinCliParameters parameters;

    public 
    StudyValidator( WebinCliParameters parameters )
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
            Study.getStudy( value, parameters.getUsername(), parameters.getPassword(), parameters.isTestMode() );
            return null;
        } catch( WebinCliException e )
        {
            return ValidationMessage.error( "MANIFEST_STUDY_SERVER_ERROR", value, e.getMessage() );
        }
    }
}
