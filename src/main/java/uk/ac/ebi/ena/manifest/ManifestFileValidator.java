/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.manifest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationMessageManager;
import uk.ac.ebi.embl.api.validation.ValidationPlanResult;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.utils.FileUtils;

@Deprecated public class ManifestFileValidator
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
        reportFile = new File( reportDir+File.separator+manifestFile.getName()+ ".report" );
//		Writer manifestrepoWriter = new PrintWriter(reportFile, "UTF-8");
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
					result.append(new ValidationResult().append(new ValidationMessage<>(Severity.ERROR, InvalidFile )));
				else
				{
				File file = new File(m.getFileName());
				if (!file.exists()&&!test)
				{
					result.append(new ValidationResult().append(new ValidationMessage<>( Severity.ERROR, fileNotExist, file ) ) );
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
		
	    FileUtils.writeReport( reportFile, result.getMessages(),  reportFile.getName() );
	    
	    return result.isValid();
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