package uk.ac.ebi.ena.utils;

import java.io.IOException;
import java.io.Writer;
import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationMessageManager;
import uk.ac.ebi.embl.api.validation.ValidationPlanResult;
import uk.ac.ebi.embl.api.validation.ValidationResult;

public class AssemblyReporter
{
	@SuppressWarnings("rawtypes") 
	public static boolean logMessages(
			String fileName,
			ValidationResult parseResult,
			ValidationPlanResult planResult,
			Writer reportWriter) throws IOException
	{
		ValidationMessageManager.addBundle(ValidationMessageManager.GENOMEASSEMBLY_VALIDATION_BUNDLE);

		for (ValidationMessage message : parseResult.getMessages(Severity.ERROR))
		{
			logMessage(message, fileName,reportWriter);
		}


		// Write validation results		

		for (ValidationResult result : planResult.getResults())
		{
			for (ValidationMessage message : result.getMessages(Severity.ERROR))
			{
				logMessage(message, fileName,reportWriter);
			}				
		}			

		return parseResult.isValid()&&planResult.isValid();
	}	
	
	@SuppressWarnings("rawtypes")
	private static void logMessage(ValidationMessage validationMessage, String fileName,Writer reportWriter) throws IOException
	{
		Severity severity = validationMessage.getSeverity();
		String message = validationMessage.getMessage();

		String[][] attrs = null;

		boolean isOrigin = !validationMessage.getOrigins().isEmpty()&&validationMessage.getOrigins().get(0)!=null;

		if (isOrigin) 
		{
			Origin origin = ((Origin) validationMessage.getOrigins().get(0));

			if (fileName != null && !fileName.isEmpty())
			{
				attrs = new String[][] { 
					{ "file name", fileName },
					{ "", origin.getOriginText() }};        		
			}
			else
			{
				attrs = new String[][] { 
					{ "", origin.getOriginText() }};        		        		
			}
		}
		else if (fileName != null && !fileName.isEmpty())
		{
			attrs = new String[][] { 
				{ "file name", fileName }};        		
		}

		reportWriter.write(severity.name()+": "+getTextMessage(message, attrs)+"\n");
	}

	
	private static String getTextMessage(String message, String[][] attrs)
	{
		if (message != null && attrs != null)
		{
			StringBuilder messageWithAttrs = new StringBuilder(message);

			messageWithAttrs.append("[{");
			for (int i = 0 ; i < attrs.length ; ++i)
			{
				if (attrs[i].length == 2)
				{
					messageWithAttrs.append(attrs[i][0]);	
					if(i==0)
						messageWithAttrs.append(": ");
					messageWithAttrs.append(attrs[i][1]);
					if(i==0)
						messageWithAttrs.append(", ");
				}
			}

			messageWithAttrs.append("}]");
			return messageWithAttrs.toString();
		}

		return message;
	}	
}
