package uk.ac.ebi.ena.webin.cli.manifest.processor;

import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;

import java.util.regex.Pattern;

public class AuthorProcessor implements ManifestFieldProcessor {



    @Override
    public ValidationResult process(ManifestFieldValue fieldValue) {
        if(null != fieldValue.getValue()) {
            fieldValue.setValue(replaceMultiplePeriods(
                    replaceMultipleSpaces(fieldValue.getValue().replaceAll(";", ""))));
        }
        return new ValidationResult();
    }

    private String replaceMultipleSpaces(String string) {
        if (string == null) {
            return null;
        }
        string = string.trim();
        Pattern pattern = Pattern.compile(" {2,}");
        return pattern.matcher(string).replaceAll(" ");
    }

    private String replaceMultiplePeriods(String string) {
        if (string == null) {
            return null;
        }
        string = string.trim();
        Pattern pattern = Pattern.compile("\\.{2,}");
        return pattern.matcher(string).replaceAll(".");
    }
}
