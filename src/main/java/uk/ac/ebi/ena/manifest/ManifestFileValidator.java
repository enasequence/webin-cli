package uk.ac.ebi.ena.manifest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationMessageManager;
import uk.ac.ebi.embl.api.validation.ValidationPlanResult;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.assembly.GenomeAssemblyFileUtils;
import uk.ac.ebi.ena.submit.ContextE;

public class ManifestFileValidator
{
	private static String InvalidFileFormat = "invalidFileFormat";
	private static String InvalidFile = "invalidFile";
	private static String fileNotExist = "fileNotExist";
	private static String infoFileNotExist = "infoFileMisssing";

    private static final String MANIFESTMESSAGEBUNDLE = "uk.ac.ebi.ena.manifest.ManifestValidationMessages";
    private boolean test=false;
    private ManifestFileReader reader= null;
    private File reportFile =null;
    
    public ManifestFileValidator() {
	this(false);
    }
	public ManifestFileValidator(boolean test) 
	{
		this.test=test;
	}
	public  boolean validate(File manifestFile,String reportDir,String context) throws FileNotFoundException, IOException
	{
        ValidationMessageManager.addBundle(MANIFESTMESSAGEBUNDLE);
        
        List<ValidationPlanResult> manifestPlanResults = new ArrayList<ValidationPlanResult>();
		List<ValidationResult> manifestParseResults = new ArrayList<ValidationResult>();
		reportFile=new File(reportDir+File.separator+manifestFile.getName()+ ".report");
		Writer manifestrepoWriter = new PrintWriter(reportFile, "UTF-8");
		reader= new ManifestFileReader();
		ValidationPlanResult result= new ValidationPlanResult();
		try {
			result = reader.read(manifestFile.toString());
		} catch (IOException e) {
		}

		List<ManifestObj> manifestRecords= reader.getManifestFileObjects();
		boolean infoFileExists =false;
		if(result.isValid())
		{
			for (ManifestObj m : manifestRecords) 
			{
				
				if(m.getFileFormat()==null||!Arrays.asList(ContextE.getContext(context.toLowerCase()).getFileFormats()).contains(m.getFileFormat()))
				   result.append(new ValidationResult().append(new ValidationMessage<>(Severity.ERROR, InvalidFileFormat,m.getFileFormatString(),context,ContextE.getContext(context.toLowerCase()).getFileFormatString())));
				
				if (m.getFileName() == null)
					result.append(new ValidationResult().append(new ValidationMessage<>(Severity.ERROR, InvalidFile)));
				else
				{
				File file = new File(m.getFileName());
				if (!file.exists()&&!test)
				{
					result.append(new ValidationResult().append(new ValidationMessage<>(Severity.ERROR, fileNotExist)));
				}
				}
				
				if(FileFormat.INFO.equals(m.getFileFormat()))
				{
					infoFileExists= true;
				}
		    }
			
			if (!infoFileExists)
			{ 
				result.append(new ValidationResult().append(new ValidationMessage<>(Severity.ERROR, infoFileNotExist)));
			}
		}
		
		manifestPlanResults.add(result);
	    return GenomeAssemblyFileUtils.writeValidationResult(manifestParseResults,manifestPlanResults, manifestrepoWriter,manifestFile.getName());
	}
	
	public ManifestFileReader getReader()
	{
		return reader;
	}
	
	public File getReportFile()
	{
		return reportFile;
	}
}