package uk.ac.ebi.ena.webin.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

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
import uk.ac.ebi.ena.upload.FtpException;
import uk.ac.ebi.ena.utils.FileUtils;
import uk.ac.ebi.ena.upload.FtpService;

public class WebinCli {
	public final static int SUCCESS = 0;
	public final static int SYSTEM_ERROR = 1;
	public final static int USER_ERROR = 2;
	public final static int VALIDATION_ERROR = 3;
	private static final String VALIDATE_SUCCESS = "The submission has been validated. " +
			"Please complete the submission process using the -upload and -submit options.";
	private static final String VALIDATE_USER_ERROR = "Submission validation has failed because of an user error. ";
	private static final String VALIDATE_SYSTEM_ERROR = "Submission validation has failed because of a system error. ";
	private static final String VALIDATE_VALIDATION_ERROR = "Submission validation has failed because of a validation error. ";
	private static final String UPLOAD_SUCCESS = "The files have been successfully uploaded. " +
			"Please complete the submission process using the -submit option.";
	private static final String UPLOAD_USER_ERROR = "Failed to upload files because of an user error. " +
			"Please correct the errors and complete the submission process using the -upload and -submit options.";
	private static final String UPLOAD_SYSTEM_ERROR = "Failed to upload files because of a system error. " +
			"Please complete the submission process using the -upload and -submit options.";
	private static final String UPLOAD_CHECK_USER_ERROR = "Failed to check if the files have been uploaded because of an user error. " +
			"Please correct the errors and use the -submit option again.";
	private static final String UPLOAD_CHECK_SYSTEM_ERROR = "Failed to check if the files have been uploaded because of a system error. " +
			"Please use the -submit option again later.";
	public static final String SUBMIT_SUCCESS = "The submission has been completed successfully.";
	private static final String SUBMIT_USER_ERROR = "The submission has failed because of an user error. " +
			"Please correct the errors and complete the submission process using the -submit option.";
	private static final String SUBMIT_SYSTEM_ERROR = "The submission has failed because of a system error. " +
			"Please use the -submit option again later.";
	public static final String INVALID_CONTEXT = "Invalid context: ";
	public static final String MISSING_CONTEXT = "Missing context or unique name.";
	public static final String INVALID_CREDENTIALS = "Invalid submission account user name or password.";
	private Params params;
	private ContextE contextE;
	private String UPLOAD_DIR = "upload";
	private String REPORTS_DIR = "validate";
	private static ManifestFileValidator manifestValidator= null;
	private static InfoFileValidator infoValidator =null;
	private static String outputDir =null;

	public static class Params	{
		@Parameter(names = "help", required = false)
		public boolean help;
		@Parameter(names = ParameterDescriptor.test, description = ParameterDescriptor.testFlagDescription, required = false)
		public boolean test;
		@Parameter(names = ParameterDescriptor.context, description = ParameterDescriptor.contextFlagDescription, required = true,validateWith = contextValidator.class)
		public String context;
		@Parameter(names = ParameterDescriptor.userName, description = ParameterDescriptor.userNameFlagDescription, required = true)
		public String userName;
		@Parameter(names = ParameterDescriptor.password, description = ParameterDescriptor.passwordFlagDescription, required = true)
		public String password;
		@Parameter(names = ParameterDescriptor.manifest, description = ParameterDescriptor.manifestFlagDescription, required = true,validateWith = manifestFileValidator.class)
		public String manifest;
		@Parameter(names = ParameterDescriptor.outputDir, description = ParameterDescriptor.outputDirFlagDescription,validateWith = OutputDirValidator.class, required = true)
		public String outputDir;
		@Parameter(names = ParameterDescriptor.validate, description = ParameterDescriptor.validateFlagDescription, required = false)
		public boolean validate;
		@Parameter(names = ParameterDescriptor.upload, description = ParameterDescriptor.uploadFlagDescription, required = false)
		public boolean upload;
		@Parameter(names = ParameterDescriptor.submit, description = ParameterDescriptor.submitFlagDescription, required = false)
		public boolean submit;
	}

	public WebinCli(Params params) {
		this.params = params;
	}

	private static String getFullPath(String path) {
		return FileSystems.getDefault().getPath(path).normalize().toAbsolutePath().toString();
	}

	public static void main(String... args) throws ValidationEngineException {
		try {
			if (args != null && args.length > 0) {
				Optional<String> found = Arrays.stream(args).filter(arg -> "-help".equalsIgnoreCase(arg))
				.findFirst();
				if (found.isPresent()) {
					usageFormatted();
					System.exit(SUCCESS);
				}
			}
			Params params = parseParameters(args);
			if (params.help) {
				System.exit(SUCCESS);
			}
			if (!params.validate &&
				!params.upload &&
				!params.submit) {
				printUsageAndExit();
			}
			params.manifest = getFullPath(params.manifest);
			File manifestFile = new File(params.manifest);
			outputDir = params.outputDir == null ? manifestFile.getParent() : params.outputDir;
			outputDir = getFullPath(outputDir);
			File reportDir= new File(outputDir + File.separator + "reports");
			if(!reportDir.exists()) {
				System.out.println("Creating report directory: " + reportDir.getCanonicalPath());
				reportDir.mkdirs();
			} else
				System.out.println("Using report directory: " + reportDir.getCanonicalPath());
			infoValidator = new InfoFileValidator();
			manifestValidator = new ManifestFileValidator();
			if(!manifestValidator.validate(manifestFile, reportDir.getAbsolutePath(), params.context)){
				System.out.println("Manifest file validation failed, please check the reporting file for errors: "+manifestValidator.getReportFile().getAbsolutePath());
				System.exit(VALIDATION_ERROR);
			}
			if(!infoValidator.validate(manifestValidator.getReader(),reportDir.getAbsolutePath(), params.context)) {
				System.out.println("Assembly info file validation failed, please check the reporting file for errors: "+infoValidator.getReportFile().getAbsolutePath());
				System.exit(VALIDATION_ERROR);
			}
			WebinCli enaValidator = new WebinCli(params);
			enaValidator.execute();
		} catch(Exception e) {
			throw new ValidationEngineException(e.getMessage());
		}
	}

	private void execute() throws ValidationEngineException {
		try {
			contextE = ContextE.valueOf(params.context);
		} catch (IllegalArgumentException e) {
			System.out.println(INVALID_CONTEXT + params.context);
			System.exit(USER_ERROR);
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
		study.getStudy(infoValidator.getentry().getStudyId(), params.userName, params.password, params.test);
		return study;
	}

	private Sample getSample() throws SampleException {
		Sample sample = new Sample();
		sample.getSample(infoValidator.getentry().getSampleId(), params.userName, params.password, params.test);
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
				case genome:
					validator = new GenomeAssemblyWebinCli(manifestValidator.getReader(),sample,study,infoValidator.getentry().getMoleculeType());
					break;
            }
			String assemblyName = infoValidator.getentry().getName().trim().replaceAll("\\s+", "_");
			validator.setReportsDir(createValidatedReportsDir(assemblyName));
			validator.setOutputDir(outputDir);
			if (validator.validate() == SUCCESS) {
				File validatedDirectory = getUploadDirectory(true, assemblyName);
                for (ManifestObj manifestObj: manifestValidator.getReader().getManifestFileObjects()) {
                	// Copy files to the validated directory.
                    Files.copy(Paths.get(manifestObj.getFileName()), 
                    		             Paths.get(validatedDirectory.getAbsolutePath() + File.separator + new File(manifestObj.getFileName()).getName()), 
                    		             StandardCopyOption.REPLACE_EXISTING);
                }
				// Gzip the files validated directory.
                for(File file:Arrays.asList(validatedDirectory.listFiles()))
                   FileUtils.gZipFile(file);
				// Create the manifest in the validated directory.
                new ManifestFileWriter().write(new File(validatedDirectory.getAbsolutePath() + File.separator + assemblyName + ".manifest"), manifestValidator.getReader().getManifestFileObjects());
                System.out.println(VALIDATE_SUCCESS);
            } else {
                System.out.println(VALIDATE_VALIDATION_ERROR + " Please check the report file under '" + outputDir + File.separator + "reports' for errors.");
                System.exit(VALIDATION_ERROR);
            }
		} catch (WebinCliException e) {
			if (WebinCliException.ErrorType.USER_ERROR.equals(e.getErrorType())) {
				System.out.println(VALIDATE_USER_ERROR + e.getMessage());
				System.exit(USER_ERROR);
			} else {
				System.out.println(VALIDATE_SYSTEM_ERROR + e.getMessage());
				System.exit(SYSTEM_ERROR);
			}
		} catch (ValidationEngineException e) {
			System.out.println(VALIDATE_VALIDATION_ERROR + e.getMessage());
			System.exit(VALIDATION_ERROR);
		} catch (Exception e) {
			System.out.println(VALIDATE_SYSTEM_ERROR + e.getMessage());
			System.exit(SYSTEM_ERROR);
		}
	}

	private void doFtpUpload() {
		String assemblyName = infoValidator.getentry().getName().trim().replaceAll("\\s+", "_");
		FtpService ftpService = new FtpService();
		try {
			ftpService.connectToFtp(params.userName, params.password);
			ftpService.ftpDirectory(getUploadDirectory(false, assemblyName).toPath(), params.context, assemblyName);
			System.out.println(UPLOAD_SUCCESS);
		} catch (WebinCliException e) {
			if (WebinCliException.ErrorType.USER_ERROR.equals(e.getErrorType())) {
				System.out.println(UPLOAD_USER_ERROR + e.getMessage());
				System.exit(USER_ERROR);
			}
			else {
				System.out.println(UPLOAD_SYSTEM_ERROR + e.getMessage());
				System.exit(SYSTEM_ERROR);
			}
		} finally {
			ftpService.disconnectFtp();
		}
	}

	private String createValidatedReportsDir(String name) {
		File reportDirectory =new File(new File(params.manifest).getParent() + File.separator + contextE + File.separator + name + File.separator + REPORTS_DIR);
		if (reportDirectory.exists())
			FileUtils.emptyDirectory(reportDirectory);
		else
			reportDirectory.mkdirs();
		return reportDirectory.getAbsolutePath();
	}

	private boolean checkFilesExistInUploadArea() {
		FtpService ftpService = null;
		boolean success = false;
		try {
			String assemblyName = infoValidator.getentry().getName().trim().replaceAll("\\s+", "_");
			ftpService = new FtpService();
			ftpService.connectToFtp(params.userName, params.password);
			success = ftpService.checkFilesExistInUploadArea(getUploadDirectory(false, assemblyName).toPath(), params.context, assemblyName);
		} catch (FtpException e) {
			if (WebinCliException.ErrorType.USER_ERROR.equals(e.getErrorType())) {
				System.out.println(UPLOAD_CHECK_USER_ERROR + e.getMessage());
				System.exit(USER_ERROR);
			}
			else {
				System.out.println(UPLOAD_CHECK_SYSTEM_ERROR + e.getMessage());
				System.exit(SYSTEM_ERROR);
			}
		} catch (Exception e) {
			System.out.println(e);
			System.exit(SYSTEM_ERROR);
		} finally {
			ftpService.disconnectFtp();
		}
		return success;
	}

	private void doSubmit() {
		try {
			String assemblyName = infoValidator.getentry().getName().trim().replaceAll("\\s+", "_");
			getUploadDirectory(false, assemblyName);
			Submit submit = new Submit(params, infoValidator.getentry());
			submit.doSubmission();
		} catch (WebinCliException e) {
			if (WebinCliException.ErrorType.USER_ERROR.equals(e.getErrorType())) {
				System.out.println(SUBMIT_USER_ERROR + e.getMessage());
				System.exit(USER_ERROR);
			} else {
				System.out.println(SUBMIT_SYSTEM_ERROR + e.getMessage());
				System.exit(SYSTEM_ERROR);
			}
		}
	}

	private File getUploadDirectory(boolean create, String name) throws FtpException {
		File validatedDirectory =new File(new File(params.manifest).getParent() + File.separator + contextE + File.separator + name + File.separator + UPLOAD_DIR);
		if (create) {
		    if (validatedDirectory.exists())
                FileUtils.emptyDirectory(validatedDirectory);
		    else
		    	validatedDirectory.mkdirs();
		} else {
		    if (!validatedDirectory.exists()) {
		        System.out.println("Directory " + validatedDirectory + " does not exist. Please use the -validate option to validate and prepare the files for submission.");
				System.exit(USER_ERROR);
		    }
		    if (validatedDirectory.list().length == 0) {
		        System.out.println("Directory "  + validatedDirectory + " does not contain any files. Please use the -validate option to validate and prepare the files for submission.");
		        System.exit(USER_ERROR);
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
			printUsageAndExit();
		}
		return params;
	}

	private static void printUsageAndExit() {
		StringBuilder usage = new StringBuilder("Invalid options: the following options are required: [-context], [-userName], [-password], [-manifest], [-outputDir]");
		usage.append("\nIn addition, please provide one of the following options:");
		usage.append("\n\t" + ParameterDescriptor.validate);
		usage.append("\n\t" + ParameterDescriptor.upload);
		usage.append("\n\t" + ParameterDescriptor.submit);
		usage.append("\nUsage: java -jar webin-cli-<version>.jar [options]");
		usage.append("\nFor full help on options use the [-help] option.");
		System.out.println(usage);
		writeReturnCodes();
		System.exit(USER_ERROR);
	}

	public static class contextValidator implements IParameterValidator {
		@Override
		public void validate(String name, String value)	throws ParameterException {
			try {
				ContextE.valueOf(value);
			} catch (IllegalArgumentException e) {
				System.out.println(INVALID_CONTEXT + value);
				System.exit(USER_ERROR);
			}
		}
	}
	
	public static class OutputDirValidator implements IParameterValidator {
		@Override
		public void validate(String name, String value)	throws ParameterException {
			File file = new File(value);
			if(!file.exists()) {
				System.out.println("The output directory " + value + " does not exist. Please provide a valid -outputDir option.");
				System.exit(USER_ERROR);
			}
		}
	}

	public static class manifestFileValidator implements IParameterValidator {
		@Override
		public void validate(String name, String value)	throws ParameterException {
			File file = new File(value);
			if(!file.exists()) {
				System.out.println("The manifest file " + value + " does not exist. Please provide a valid -manifest option.");
				System.exit(USER_ERROR);
			}
		}
	}

	private static void writeReturnCodes()	{
		HashMap<Integer, String> returnCodeMap = new HashMap<>();
		returnCodeMap.put(SUCCESS, "SUCCESS");
		returnCodeMap.put(SYSTEM_ERROR, "INTERNAL ERROR");
		returnCodeMap.put(USER_ERROR, "USER ERROR");
		returnCodeMap.put(VALIDATION_ERROR, "VALIDATION ERROR");
		System.out.println("Exit codes: " + returnCodeMap.toString());
	}

	private static void usageFormatted() {
		StringBuilder options = new StringBuilder(ParameterDescriptor.context + ParameterDescriptor.contextFlagDescription);;
		options.append("\n" + ParameterDescriptor.userName + ParameterDescriptor.userNameFlagDescription);
		options.append("\n" + ParameterDescriptor.password + ParameterDescriptor.passwordFlagDescription);
		options.append("\n" + ParameterDescriptor.manifest + ParameterDescriptor.manifestFlagDescription);;
		options.append("\n" + ParameterDescriptor.outputDir + ParameterDescriptor.outputDirFlagDescription);;
		options.append("\n" + ParameterDescriptor.validate + ParameterDescriptor.validateFlagDescription);;
		options.append("\n" + ParameterDescriptor.upload + ParameterDescriptor.uploadFlagDescription);;
		options.append("\n" + ParameterDescriptor.submit + ParameterDescriptor.submitFlagDescription);;
		System.out.println(options);
	}
}
