package uk.ac.ebi.ena.manifest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationMessageManager;
import uk.ac.ebi.embl.api.validation.ValidationPlanResult;
import uk.ac.ebi.embl.api.validation.ValidationResult;

public class ManifestFileValidator
{
	private static String InvalidFileFormat = "invalidFileFormat";
	private static String InvalidFile = "invalidFile";
	private static String fileNotExist = "fileNotExist";
    private static final String MANIFESTMESSAGEBUNDLE = "uk.ac.ebi.ena.manifest.ManifestValidationMessages";
    private boolean test=false;
    
    public ManifestFileValidator() {
	this(false);
    }
	public ManifestFileValidator(boolean test) 
	{
		this.test=test;
	}
	public ValidationPlanResult validate(File manifestFile,String context) throws FileNotFoundException, IOException
	{
        ValidationMessageManager.addBundle(MANIFESTMESSAGEBUNDLE);
		ManifestFileReader reader= new ManifestFileReader();
//		ValidationPlanResult result=reader.read(manifestFile);
		ValidationPlanResult result= null;
		try {
			result = reader.read(manifestFile.toString());
		} catch (Exception e) {

		}
		List<ManifestObj> manifestRecords= reader.getManifestFileObjects();
		if(result.isValid())
		{
			for (ManifestObj m : manifestRecords) 
			{
				if(m.getFileFormat()==null)
					result.append(new ValidationResult().append(new ValidationMessage<>(Severity.ERROR, InvalidFileFormat,m.getFileName())));
				if (m.getFileName() == null)
					result.append(new ValidationResult().append(new ValidationMessage<>(Severity.ERROR, InvalidFile)));
				File file = new File(m.getFileName());
				if (!file.exists()&&!test)
					result.append(new ValidationResult().append(new ValidationMessage<>(Severity.ERROR, fileNotExist)));
		    }
		}
		return result;
	}
	
	
}