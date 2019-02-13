package uk.ac.ebi.ena.manifest.processor;

import java.util.regex.Pattern;

import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.ena.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.manifest.ManifestFieldValue;

public class 
ASCIIFileNameProcessor implements ManifestFieldProcessor 
{
    static final Pattern pattern = Pattern.compile( "^([\\p{Alnum}]|\\\\|\\]|\\[|#|-|_|\\.|,|\\/|:|@|\\+| |\\(|\\)|'|~|<|%|\\?)+$" );

    @Override
    public ValidationMessage<Origin> 
    process( ManifestFieldValue fieldValue )
    {
        if( pattern.matcher( fieldValue.getValue() ).matches() )
            return null;

        return ValidationMessage.error( "MANIFEST_INVALID_FILE_NAME", fieldValue.getName(), fieldValue.getValue(), pattern.pattern() );
    }
}
