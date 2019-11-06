package uk.ac.ebi.ena.webin.cli.context.taxrefset;

import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.api.Validator;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;

public class PlaceHolderForTaxRefSetValidator implements Validator<Manifest,ValidationResponse> {
    @Override
    public ValidationResponse validate(Manifest manifest) {
        return null;
    }
}
