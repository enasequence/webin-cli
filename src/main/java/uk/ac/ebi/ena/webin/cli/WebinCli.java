package uk.ac.ebi.ena.webin.cli;

import java.io.*;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

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
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.submit.Submit;
import uk.ac.ebi.ena.utils.FileUtils;
import uk.ac.ebi.ena.upload.FtpService;
import uk.ac.ebi.ena.version.Version;

public class WebinCli {
	public final static int SUCCESS = 0;
	public final static int SYSTEM_ERROR = 1;
	public final static int USER_ERROR = 2;
	public final static int VALIDATION_ERROR = 3;
	private static final String VALIDATE_SUCCESS = "The submission has been validated. " +
			"Please complete the submission process using the -upload and -submit options.";
	private static final String VALIDATE_USER_ERROR = "Submission validation has failed because of an user error. " +
			"Please check the report directory for errors: ";
	private static final String VALIDATE_SYSTEM_ERROR = "Submission validation has failed because of a system error. ";

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

	public static final String AUTHENTICATION_ERROR = "Invalid submission account user name or password.";

	public static final String INVALID_CONTEXT = "Invalid context: ";
	public static final String MISSING_CONTEXT = "Missing context or unique name.";
	private final static String INVALID_VERSION = "Your current application version webin-cli __VERSION__.jar is out of date, please download the latest version from https://github.com/enasequence/webin-cli/releases.";
	private final static String INVALID_MANIFEST = "Manifest file validation failed. Please check the report file for errors: ";
	private final static String INVALID_INFO = "Info file validation failed. Please check the report file for errors: ";

	private Params params;
	private ContextE contextE;
	private final static String UPLOAD_DIR = "upload";
	private final static String VALIDATE_DIR = "validate";
	private final static String INFO_FIELD = "INFO";
	private final static String ASSEMBLYNAME_FIELD = "ASSEMBLYNAME";

	private static ManifestFileValidator manifestValidator = null;
	private static InfoFileValidator infoValidator = null;

	private static String outputDir;
	private static String reportDir;
	private static String webinCliReportFile;
	private static String infoReportFile;


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
		@Parameter(names = ParameterDescriptor.outputDir, description = ParameterDescriptor.outputDirFlagDescription,validateWith = OutputDirValidator.class)
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

	public static void main(String... args) {
		try {
			if (args != null && args.length > 0) {
				Optional<String> found = Arrays.stream(args).filter(arg -> "-help".equalsIgnoreCase(arg))
						.findFirst();
				if (found.isPresent()) {
					printUsageHelpAndExit();
				}
			}
			Params params = parseParameters(args);
			if (!params.validate &&
				!params.upload &&
				!params.submit) {
				printUsageErrorAndExit();
			}

			checkVersion();

			String name = peekInfoFileForName(peekManifestForInfoFile(params.manifest));
			params.manifest = getFullPath(params.manifest);
			File manifestFile = new File(params.manifest);
			outputDir = params.outputDir == null ? manifestFile.getParent() : params.outputDir;
			outputDir = getFullPath(outputDir);
			createValidatedReportsDir(params.context, name);
			infoValidator = new InfoFileValidator();
			manifestValidator = new ManifestFileValidator();
			if (!manifestValidator.validate(manifestFile, reportDir, params.context)) {
				throw WebinCliException.createUserError(INVALID_MANIFEST, manifestValidator.getReportFile().getAbsolutePath());
			}
			if (!infoValidator.validate(manifestValidator.getReader(), reportDir, params.context)) {
				throw WebinCliException.createUserError(INVALID_INFO, infoValidator.getReportFile().getAbsolutePath());
			}
			infoReportFile = infoValidator.getReportFile().getAbsolutePath();
			createWebinCliReportFile();
			WebinCli webinCli = new WebinCli(params);
			webinCli.execute();
			System.exit(SUCCESS);
		} catch (WebinCliException e) {
            writeMessage(e.getMessage());
			switch (e.getErrorType()) {
				case SYSTEM_ERROR:
					System.exit(SYSTEM_ERROR);
				case USER_ERROR:
					System.exit(USER_ERROR);
				case VALIDATION_ERROR:
					System.exit(VALIDATION_ERROR);
			}
		} catch(Exception e) {
			writeMessage(e.getMessage());
			System.exit(SYSTEM_ERROR);
		}
	}

	private void execute() throws ValidationEngineException {
		try {
			contextE = ContextE.valueOf(params.context);
		} catch (IllegalArgumentException e) {
			throw WebinCliException.createUserError(INVALID_CONTEXT, params.context);
		}
		if (params.validate)
			doValidation();
		if (params.upload)
			doFtpUpload();
		if (params.submit && checkFilesExistInUploadArea())
			doSubmit();
	}

	private Study getStudy() {
		Study study = new Study();
		try {
			study.getStudy(infoValidator.getentry().getStudyId(), params.userName, params.password, params.test);
		}
		catch (WebinCliException e) {
			writeMessageIntoInfoReport(e.getMessage());
			throw e;
		}
		return study;
	}

	private Sample getSample() {
		Sample sample = new Sample();
		try {
			sample.getSample(infoValidator.getentry().getSampleId(), params.userName, params.password, params.test);
		}
		catch (WebinCliException e) {
			writeMessageIntoInfoReport(e.getMessage());
			throw e;
		}
		return sample;
	}

	private void doValidation() {
		Study study = getStudy();
		Sample sample = getSample();

		WebinCliInterface validator = null;
		switch(contextE) {
			case transcriptome:
				validator = new TranscriptomeAssemblyWebinCli(manifestValidator.getReader(), sample, study);
				break;
			case genome:
				validator = new GenomeAssemblyWebinCli(manifestValidator.getReader(),sample,study,infoValidator.getentry().getMoleculeType());
				break;
		}
		String assemblyName = infoValidator.getentry().getName().trim().replaceAll("\\s+", "_");
		validator.setReportsDir(reportDir);

		int validatorResult;
		try {
			validatorResult = validator.validate();
		} catch (ValidationEngineException e) {
			throw WebinCliException.createSystemError(VALIDATE_SYSTEM_ERROR, e.getMessage());
		}
		if (validatorResult != SUCCESS) {
			throw WebinCliException.createValidationError(VALIDATE_USER_ERROR, reportDir + File.separator + "validate");
		}

		try {
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
			writeMessage(VALIDATE_SUCCESS);
		} catch (IOException | NoSuchAlgorithmException e) {
			throw WebinCliException.createSystemError(VALIDATE_SYSTEM_ERROR, e.getMessage());
		}
	}

	private void doFtpUpload() {
		String assemblyName = infoValidator.getentry().getName().trim().replaceAll("\\s+", "_");
		FtpService ftpService = new FtpService();
		try {
			ftpService.connectToFtp(params.userName, params.password);
			ftpService.ftpDirectory(getUploadDirectory(false, assemblyName).toPath(), params.context, assemblyName);
			writeMessage(UPLOAD_SUCCESS);
		} catch (WebinCliException e) {
			e.throwAddMessage(UPLOAD_USER_ERROR, UPLOAD_SYSTEM_ERROR);
		} finally {
			ftpService.disconnectFtp();
		}
	}

	private static void createValidatedReportsDir(String context, String assemblyName) throws Exception {
		File reportDirectory = new File(outputDir + File.separator + context + File.separator + assemblyName + File.separator + VALIDATE_DIR);
		if (reportDirectory.exists())
			FileUtils.emptyDirectory(reportDirectory);
		else
			reportDirectory.mkdirs();
		reportDir = reportDirectory.getAbsolutePath();
	}

	private static void createWebinCliReportFile() throws IOException {
		Path reportPath = Paths.get(reportDir + File.separator + "webin-cli.report");
		if (Files.exists(reportPath))
			Files.delete(reportPath);
		Files.createFile(reportPath);
		webinCliReportFile = reportPath.toFile().getAbsolutePath();
	}

	private boolean checkFilesExistInUploadArea() {
		FtpService ftpService = null;
		boolean success = false;
		try {
			String assemblyName = infoValidator.getentry().getName().trim().replaceAll("\\s+", "_");
			ftpService = new FtpService();
			ftpService.connectToFtp(params.userName, params.password);
			success = ftpService.checkFilesExistInUploadArea(getUploadDirectory(false, assemblyName).toPath(), params.context, assemblyName);
		} catch (WebinCliException e) {
			e.throwAddMessage(UPLOAD_CHECK_USER_ERROR, UPLOAD_CHECK_SYSTEM_ERROR);
		} finally {
			ftpService.disconnectFtp();
		}
		return success;
	}

	private void doSubmit() {
		try {
			String assemblyName = infoValidator.getentry().getName().trim().replaceAll("\\s+", "_");
			getUploadDirectory(false, assemblyName);
			Submit submit = new Submit(params,outputDir, infoValidator.getentry());
			submit.doSubmission();
		} catch (WebinCliException e) {
			e.throwAddMessage(SUBMIT_USER_ERROR, SUBMIT_SYSTEM_ERROR);
		}
	}

	private File getUploadDirectory(boolean create, String name) {
		File validatedDirectory =new File(new File(outputDir) + File.separator + contextE + File.separator + name + File.separator + UPLOAD_DIR);
		if (create) {
		    if (validatedDirectory.exists())
                FileUtils.emptyDirectory(validatedDirectory);
		    else
		    	validatedDirectory.mkdirs();
		} else {
		    if (!validatedDirectory.exists()) {
				throw WebinCliException.createUserError("Directory " + validatedDirectory + " does not exist. Please use the -validate option to validate and prepare the files for submission."
                );
		    }
		    if (validatedDirectory.list().length == 0) {
				throw WebinCliException.createUserError("Directory "  + validatedDirectory + " does not contain any files. Please use the -validate option to validate and prepare the files for submission."
                );
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
			printUsageErrorAndExit();
		}
		return params;
	}

	private static void printUsageErrorAndExit() {
		StringBuilder usage = new StringBuilder("Invalid options: the following options are required: [-context], [-userName], [-password], [-manifest], [-outputDir]");
		usage.append("\nIn addition, please provide one of the following options:");
		usage.append("\n\t" + ParameterDescriptor.validate);
		usage.append("\n\t" + ParameterDescriptor.upload);
		usage.append("\n\t" + ParameterDescriptor.submit);
		usage.append("\nUsage: java -jar webin-cli-<version>.jar [options]");
		usage.append("\nFor full help on options use the [-help] option.");
		writeMessage(usage.toString());
		writeReturnCodes();
		System.exit(USER_ERROR);
	}

	private static void printUsageHelpAndExit() {
		StringBuilder usage = new StringBuilder(ParameterDescriptor.context + ParameterDescriptor.contextFlagDescription);;
		usage.append("\n" + ParameterDescriptor.userName + ParameterDescriptor.userNameFlagDescription);
		usage.append("\n" + ParameterDescriptor.password + ParameterDescriptor.passwordFlagDescription);
		usage.append("\n" + ParameterDescriptor.manifest + ParameterDescriptor.manifestFlagDescription);;
		usage.append("\n" + ParameterDescriptor.outputDir + ParameterDescriptor.outputDirFlagDescription);;
		usage.append("\n" + ParameterDescriptor.validate + ParameterDescriptor.validateFlagDescription);;
		usage.append("\n" + ParameterDescriptor.upload + ParameterDescriptor.uploadFlagDescription);;
		usage.append("\n" + ParameterDescriptor.submit + ParameterDescriptor.submitFlagDescription);;
		writeMessage(usage.toString());
		writeReturnCodes();
		System.exit(SUCCESS);
	}

	private static void writeReturnCodes()	{
		HashMap<Integer, String> returnCodeMap = new HashMap<>();
		returnCodeMap.put(SUCCESS, "SUCCESS");
		returnCodeMap.put(SYSTEM_ERROR, "INTERNAL ERROR");
		returnCodeMap.put(USER_ERROR, "USER ERROR");
		returnCodeMap.put(VALIDATION_ERROR, "VALIDATION ERROR");
		writeMessage("Exit codes: " + returnCodeMap.toString());
	}

	public static class contextValidator implements IParameterValidator {
		@Override
		public void validate(String name, String value)	throws ParameterException {
			try {
				ContextE.valueOf(value);
			} catch (IllegalArgumentException e) {
				throw WebinCliException.createUserError(INVALID_CONTEXT, value
                );
			}
		}
	}
	
	public static class OutputDirValidator implements IParameterValidator {
		@Override
		public void validate(String name, String value)	throws ParameterException {
			File file = new File(value);
			if(!file.exists()) {
				throw WebinCliException.createUserError("The output directory " + value + " does not exist. Please provide a valid -outputDir option."
                );
			}
		}
	}

	public static class manifestFileValidator implements IParameterValidator {
		@Override
		public void validate(String name, String value)	throws ParameterException {
			File file = new File(value);
			if(!file.exists()) {
				throw WebinCliException.createUserError("The manifest file " + value + " does not exist. Please provide a valid -manifest option."
                );
			}
		}
	}

	private static String peekManifestForInfoFile(String manifest) throws Exception {
		try (Stream<String> stream = Files.lines(Paths.get(manifest))) {
			Optional<String> optional = stream.filter(line -> line.toUpperCase().startsWith(INFO_FIELD))
					.findFirst();
			if (optional.isPresent())
				return optional.get().substring(INFO_FIELD.length()).trim();
			else {
				throw WebinCliException.createUserError("Manifest file " + manifest + " is missing the " + INFO_FIELD + " field."
                );
			}
		}
	}

	private static String peekInfoFileForName(String info) throws Exception {
		InputStream fileIs = Files.newInputStream(Paths.get(info));
		BufferedInputStream bufferedIs = new BufferedInputStream(fileIs);
		GZIPInputStream gzipIs = new GZIPInputStream(bufferedIs);
		BufferedReader reader = new BufferedReader(new InputStreamReader(gzipIs));
		Optional<String> optional = reader.lines().filter(line -> line.toUpperCase().startsWith(ASSEMBLYNAME_FIELD))
				.findFirst();
		if (optional.isPresent())
			return optional.get().substring(ASSEMBLYNAME_FIELD.length()).trim().replaceAll("\\s+", "_");
		else {
			throw WebinCliException.createUserError("Info file " + info + " is missing the " + ASSEMBLYNAME_FIELD + " field."
            );
		}
	}

	private static void checkVersion() {
		String version = WebinCli.class.getPackage().getImplementationVersion();
		Version versionService = new Version();
		if (!versionService.isVersionValid(version)) {
			throw WebinCliException.createUserError(INVALID_VERSION.replaceAll("__VERSION__", version));
		}
	}

	/**
	 * Writes messages to the webin cli report file and to the console. If there is no
	 * webin-cli.report file then writes only to the console.
	 */
	public static void writeMessage(String message) {
		if (message == null || message.isEmpty()) {
			return;
		}
		writeMessageIntoConsole(message);
		if (webinCliReportFile != null) {
			try {
				Files.write(Paths.get(webinCliReportFile), message.getBytes(), StandardOpenOption.APPEND);
			} catch (IOException e) {
			}
		}
	}

	public static void writeMessageIntoInfoReport(String message) {
		if (message == null || message.isEmpty()) {
			return;
		}
		if (infoReportFile != null) {
			try {
				Files.write(Paths.get(infoReportFile), message.getBytes(), StandardOpenOption.APPEND);
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Writes messages to the console.
	 */
	private static void writeMessageIntoConsole(String message) {
		System.out.println(message);
	}
}
