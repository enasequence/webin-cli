package uk.ac.ebi.ena.webin.cli.manifest.processor;

import java.util.regex.Pattern;

import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;

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

        return WebinCliMessage.error(WebinCliMessage.Manifest.INVALID_FILE_NAME_ERROR, fieldValue.getName(), fieldValue.getValue(), pattern.pattern() );
    }
}
