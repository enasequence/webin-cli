package uk.ac.ebi.ena.utils;

import java.io.IOException;
import java.io.Writer;

import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationMessageManager;
import uk.ac.ebi.embl.api.validation.ValidationPlanResult;
import uk.ac.ebi.embl.api.validation.ValidationResult;

public class AssemblyReporter
{
	public static boolean logMessages(
			String fileName,
			ValidationResult parseResult,
			ValidationPlanResult planResult,
			Writer reportWriter) throws IOException
	{
		ValidationMessageManager.addBundle(ValidationMessageManager.GENOMEASSEMBLY_VALIDATION_BUNDLE);
		parseResult.setDefaultMessageFormatter(ValidationMessage.TEXT_TIME_MESSAGE_FORMATTER_TRAILING_LINE_END);
		parseResult.writeMessages(reportWriter, Severity.ERROR, fileName);	
		

	
		for (ValidationResult result : planResult.getResults())
		{
			result.writeMessages(reportWriter, Severity.ERROR, fileName);	
		}			

		return parseResult.isValid()&&planResult.isValid();
	}	
	
}
