package uk.ac.ebi.ena.webin.cli;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.ena.assembly.GenomeAssemblyWebinCli;
import uk.ac.ebi.ena.assembly.InfoFileValidator;
import uk.ac.ebi.ena.assembly.TranscriptomeAssemblyWebinCli;
import uk.ac.ebi.ena.manifest.*;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.sample.SampleException;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.study.StudyException;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.submit.Submit;
import uk.ac.ebi.ena.submit.SubmitException;
import uk.ac.ebi.ena.upload.FtpException;
import uk.ac.ebi.ena.utils.FileUtils;
import uk.ac.ebi.ena.upload.FtpService;

public class WebinCli {
	public final static int SUCCESS = 0;
	public final static int FLAILED_VALIDATION = 3;
	private Params params;
	private ContextE contextE;
	private static ManifestFileValidator manifestValidator= null;
	private static InfoFileValidator infoValidator =null;
	private static String outputDir =null;

	public static class Params	{
		@Parameter(names = ParameterDescriptor.context, description = ParameterDescriptor.contextFlagDescription, required = true,validateWith = contextValidator.class)
		public String context;
		@Parameter(names = ParameterDescriptor.outputDir, description = ParameterDescriptor.outputDirFlagDescription,validateWith = OutputDirValidator.class)
		public String outputDir;
		@Parameter(names = ParameterDescriptor.userName, description = ParameterDescriptor.userNameFlagDescription, required = true)
		public String userName;
		@Parameter(names = ParameterDescriptor.password, description = ParameterDescriptor.passwordFlagDescription, required = true)
		public String password;
		@Parameter(names = ParameterDescriptor.validate, description = ParameterDescriptor.validateFlagDescription, required = false)
		public boolean validate;
		@Parameter(names = ParameterDescriptor.submit, description = ParameterDescriptor.submitFlagDescription, required = false)
		public boolean submit;
		@Parameter(names = ParameterDescriptor.upload, description = ParameterDescriptor.uploadFlagDescription, required = false)
		public boolean upload;
		@Parameter(names = ParameterDescriptor.manifest, description = ParameterDescriptor.manifestFlagDescription, required = true,validateWith = manifestFileValidator.class)
		public String manifest;
	}

	public WebinCli(Params params) {
		this.params = params;
	}

	public static void main(String... args) throws ValidationEngineException {
		try {
			Params params = parseParameters(args);
			File manifestFile = new File(params.manifest);
			outputDir = params.outputDir == null ? manifestFile.getParent() : params.outputDir;
			File reportDir= new File(outputDir+File.separator+"reports");
			if(!reportDir.exists())
				reportDir.mkdirs();
			infoValidator= new InfoFileValidator();
			manifestValidator = new ManifestFileValidator();
			if(!manifestValidator.validate(manifestFile, reportDir.getAbsolutePath(),params.context))
			{
				System.err.println("Manifest file validation failed,please check the reporting file for errors: "+manifestValidator.getReportFile().getAbsolutePath());
				System.exit(3);
			}
			if(!infoValidator.validate(manifestValidator.getReader(),reportDir.getAbsolutePath(),params.context))
			{
				System.err.println("Assembly info file validation failed,please check the reporting file for errors: "+infoValidator.getReportFile().getAbsolutePath());
				System.exit(3);
			}
			WebinCli enaValidator = new WebinCli(params);
			enaValidator.execute();
		} catch(Exception e){
			throw new ValidationEngineException(e.getMessage());
		}
	}

	private void execute() throws ValidationEngineException {
		
		try {
			contextE = ContextE.valueOf(params.context);
		} catch (IllegalArgumentException e) {
			System.err.println("Unsupported context parameter supplied: " + params.context);
			System.exit(2);
		}
		if (params.validate)
			doValidation();
		if (params.upload)
			doFtpUpload();
		if (params.submit && checkFilesExistInUploadArea())
			doSubmit();
	}

	private Study getStudy() throws StudyException {
		Study study = new Study();
		study.getStudy(infoValidator.getentry().getStudyId(), params.userName, params.password);
		return study;
	}

	private Sample getSample() throws SampleException {
		Sample sample = new Sample();
		sample.getSample(infoValidator.getentry().getSampleId(), params.userName, params.password);
		return sample;
	}

	private void doValidation() {
		try {
			WebinCliInterface validator = null;
			Study study = getStudy();
			  Sample sample=getSample();
			  switch(contextE) {
                case transcriptome:
                    validator = new TranscriptomeAssemblyWebinCli(manifestValidator.getReader(), sample, study);
                    break;
				case assembly:
					validator = new GenomeAssemblyWebinCli(manifestValidator.getReader(),sample,study,infoValidator.getentry().getMoleculeType());
					break;
            }
			validator.setOutputDir(outputDir);
			if (validator.validate() == SUCCESS) 
			{
                String assemblyName = infoValidator.getentry().getName().trim().replaceAll(" ", "");
				File validatedDirectory = getValidatedDirectory(true, assemblyName);
                for (ManifestObj manifestObj: manifestValidator.getReader().getManifestFileObjects()) {
                    Files.copy(Paths.get(manifestObj.getFileName()), 
                    		             Paths.get(validatedDirectory.getAbsolutePath() + File.separator + new File(manifestObj.getFileName()).getName()), 
                    		             StandardCopyOption.REPLACE_EXISTING);
                }
                for(File file:Arrays.asList(validatedDirectory.listFiles()))
                {
                   FileUtils.gZipFile(file);

                }
                new ManifestFileWriter().write(new File(validatedDirectory.getAbsolutePath() + File.separator + assemblyName + ".manifest"), 
                		                        manifestValidator.getReader().getManifestFileObjects());
                System.out.println("All file(s) have successfully passed validation.");
            } else {
                System.out.println("Validation has failed, please check the report file under " + outputDir + " for erros.");
                System.exit(3);
            }
		} catch (SampleException e) {
			System.out.println(e);
			System.exit(1);
		} catch (StudyException e) {
			System.out.println(e);
			System.exit(1);
		} catch (ValidationEngineException e) {
			System.out.println("Validation has failed, please check the report file under " + params.outputDir + " for erros.");
			System.exit(3);
		} catch (Exception e) {
			System.out.println("Validation has failed due to " + e.getMessage());
			System.exit(3);
		}
	}

	private void doFtpUpload() {
		String assemblyName = infoValidator.getentry().getName();
		FtpService ftpService = new FtpService(new File(params.manifest).getParent());
		try {
			ftpService.connectToFtp(params.userName, params.password);
			ftpService.ftpDirectory(getValidatedDirectory(false, assemblyName).toPath(), params.context, assemblyName);
			System.out.println("All file(s) have successfully been uploaded.");
		} catch (FtpException e) {
			System.out.println("Failed to upload files, please try again later, reason: " + e);
			System.exit(3);
		} finally {
			ftpService.disconnectFtp();
		}
	}

	private boolean checkFilesExistInUploadArea() {
		FtpService ftpService = null;
		boolean success = false;
		try {
			String assemblyName = infoValidator.getentry().getName();
			ftpService = new FtpService(new File(params.manifest).getParent());
			ftpService.connectToFtp(params.userName, params.password);
			success = ftpService.checkFilesExistInUploadArea(getValidatedDirectory(false, assemblyName).toPath(), params.context, assemblyName);
		} catch (Exception e) {
			System.out.println(e);
			System.exit(3);
		} finally {
			ftpService.disconnectFtp();
		}
		return success;
	}

	private void doSubmit() {
		try {
			String assemblyName = infoValidator.getentry().getName();
			getValidatedDirectory(false, assemblyName);
			Submit submit = new Submit(params, infoValidator.getentry());
			submit.doSubmission();
			System.out.println("All file(s) have successfully been submitted.");
		} catch (FtpException e) {
			System.out.println(e);
			System.exit(1);
		} catch (SubmitException e) {
			System.out.println(e);
			System.exit(1);
		}
	}

	private File getValidatedDirectory(boolean create, String name) throws FtpException {
		File validatedDirectory =new File(new File(params.manifest).getParent() + File.separator + contextE + File.separator + name);
		if (create) {
		    if (validatedDirectory.exists())
                FileUtils.emptyDirectory(validatedDirectory);
		    else
		    	validatedDirectory.mkdirs();
		} else {
		    if (!validatedDirectory.exists()) {
		        System.out.println("Directory does not exist, -validate option required." + validatedDirectory);
		        System.exit(3);
		    }
		    if (validatedDirectory.list().length == 0) {
		        System.out.println("Directory does not contain any files, -validate option required." + validatedDirectory);
		        System.exit(3);
		    }
		}
		return validatedDirectory;
	}

	private static Params parseParameters(String... args) {
		Params params = new Params();
		JCommander jCommander = new JCommander(params);
		try {
			jCommander.parse(args);
		} catch (Exception e) {
			System.err.println("Invalid options: " + e.getMessage());
			jCommander.usage();
			writeReturnCodes();
			System.exit(2);
		}
		return params;
	}

	public static class contextValidator implements IParameterValidator {
		@Override
		public void validate(String name, String value)	throws ParameterException {
			try {
				ContextE.valueOf(value);
			} catch (IllegalArgumentException e) {
				System.err.println("Unsupported context parameter supplied: " + value);
				System.exit(2);
			}
		}
	}
	
	public static class OutputDirValidator implements IParameterValidator {
		@Override
		public void validate(String name, String value)	throws ParameterException {
			File file = new File(value);
			if(!file.exists()) {
				System.err.println("Output Directory Does not exist - exiting");
				System.exit(2);
			}
		}
	}

	public static class manifestFileValidator implements IParameterValidator {
		@Override
		public void validate(String name, String value)	throws ParameterException {
			File file = new File(value);
			if(!file.exists()) {
				System.err.println("Manifest File Does not exist - exiting");
				System.exit(2);
			}
		}
	}

	private static void writeReturnCodes()	{
		HashMap<Integer, String> returnCodeMap = new HashMap<Integer, String>();
		returnCodeMap.put(0, "SUCCESS");
		returnCodeMap.put(1, "INTERNAL ERROR");
		returnCodeMap.put(2, "INVALID INPUT");
		returnCodeMap.put(3, "VALIDATION ERROR");
		System.out.println("Return Codes: " + returnCodeMap.toString());
	}
}
