package uk.ac.ebi.ena.manifest.processor;

import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.ena.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;

import java.util.List;

public class FileSuffixProcessor implements ManifestFieldProcessor {

    private final List<String> suffixes;

    public FileSuffixProcessor(List<String> suffixes ) {
        this.suffixes = suffixes;
    }

    @Override
    public ValidationMessage<Origin> process(ManifestFieldValue fieldValue) {

        if( null == suffixes || suffixes.isEmpty() )
            return null;

        for( String suffix : suffixes )
        {
            if( fieldValue.getValue().endsWith( suffix ) )
                return null;
        }

        return WebinCliMessage.error(WebinCliMessage.Manifest.INVALID_FILE_SUFFIX_ERROR,
                fieldValue.getName(),
                fieldValue.getValue(),
                String.join(", ", suffixes));
    }
}
