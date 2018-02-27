package uk.ac.ebi.ena.webin.cli;

import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.ena.manifest.ManifestObj;

import java.util.List;

public interface WebinCliInterface {
	int validate() throws ValidationEngineException;
	void setOutputDir(String outputDir);
	void setReportsDir(String reportDir);
}
