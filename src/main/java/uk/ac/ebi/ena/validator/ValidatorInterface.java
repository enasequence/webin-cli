package uk.ac.ebi.ena.validator;

import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.ena.manifest.ManifestObj;

import java.util.List;

public interface ValidatorInterface {
	int validate() throws ValidationEngineException;
	void setOutputDir(String outputDir);
}
