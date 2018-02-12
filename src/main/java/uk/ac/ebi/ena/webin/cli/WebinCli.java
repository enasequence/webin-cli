package uk.ac.ebi.ena.webin.cli;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationPlanResult;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.assembly.GenomeAssemblyFileUtils;
import uk.ac.ebi.ena.assembly.GenomeAssemblyWebinCli;
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
	private ManifestFileReader manifestFileReader;
	private AssemblyInfoEntry assemblyInfoEntry;
	private Params params;
	private ContextE contextE;

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

	public WebinCli(ManifestFileReader manifestFileReader, Params params) {
		this.manifestFileReader = manifestFileReader;
		this.params = params;
	}

	public static void main(String... args) throws ValidationEngineException {
		try {
			Params params = parseParameters(args);
			ManifestFileReader manifestFileReader = readAndValidateManifestFile(params);
			WebinCli enaValidator = new WebinCli(manifestFileReader, params);
			enaValidator.execute();
		} catch(Exception e){
			throw new ValidationEngineException(e.getMessage());
		}
	}

	private void execute() throws ValidationEngineException {
		assemblyInfoEntry = getAssemblyInfo(manifestFileReader.getFilenameFromManifest(FileFormat.INFO));
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
		study.getStudy(assemblyInfoEntry.getStudyId(), params.userName, params.password);
		return study;
	}

	private Sample getSample() throws SampleException {
		Sample sample = new Sample();
		sample.getSample(assemblyInfoEntry.getSampleId(), params.userName, params.password);
		return sample;
	}

	private void doValidation() {
		try {
			WebinCliInterface validator = null;
			Study study = getStudy();
			  Sample sample=getSample();
			  switch(contextE) {
                case transcriptome:
					validateInfoFileForTranscriptome();
                    validator = new TranscriptomeAssemblyWebinCli(manifestFileReader, sample, study);
                    break;
				case assembly:
					validator = new GenomeAssemblyWebinCli(manifestFileReader,sample,study);
					break;
            }
			validator.setOutputDir(params.outputDir);
			if (validator.validate() == SUCCESS) {
                String assemblyName = assemblyInfoEntry.getName();
				Path validatedDirectory = getValidatedDirectory(true, assemblyName);
                for (ManifestObj manifestObj: manifestFileReader.getManifestFileObjects()) {
                    String srcFile = FileUtils.gZipFile(new File(manifestObj.getFileName()));
                    Files.copy(Paths.get(srcFile), Paths.get(validatedDirectory.toFile().getAbsolutePath() + File.separator + new File(srcFile).getName()), StandardCopyOption.REPLACE_EXISTING);
                }
                new ManifestFileWriter().write(new File(validatedDirectory.toFile().getAbsolutePath() + File.separator + assemblyName + ".manifest"), manifestFileReader.getManifestFileObjects());
                System.out.println("All file(s) have successfully passed validation.");
            } else {
                System.out.println("Validation has failed, please check the report file under " + params.outputDir + " for erros.");
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

	private void validateInfoFileForTranscriptome() throws ValidationEngineException {
		boolean INFO_FILE_ERROR = false;
		if (assemblyInfoEntry.getName() == null || assemblyInfoEntry.getName().isEmpty()) {
			System.out.println("Field ASSEMBLYNAME is missing from info file");
			INFO_FILE_ERROR = true;
		}
		if (assemblyInfoEntry.getPlatform() == null || assemblyInfoEntry.getPlatform().isEmpty()) {
			System.out.println("Field PLATFORM is missing from info file");
			INFO_FILE_ERROR = true;
		}
		if (assemblyInfoEntry.getProgram() == null || assemblyInfoEntry.getProgram().isEmpty()) {
			System.out.println("Field PROGRAM is missing from info file");
			INFO_FILE_ERROR = true;
		}
		if (INFO_FILE_ERROR)
			System.exit(3);
	}

	private void doFtpUpload() {
		String assemblyName = assemblyInfoEntry.getName();
		FtpService ftpService = new FtpService(new File(params.manifest).getParent());
		try {
			ftpService.connectToFtp(params.userName, params.password);
			ftpService.ftpDirectory(getValidatedDirectory(false, assemblyName), params.context, assemblyName);
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
			String assemblyName = assemblyInfoEntry.getName();
			ftpService = new FtpService(new File(params.manifest).getParent());
			ftpService.connectToFtp(params.userName, params.password);
			success = ftpService.checkFilesExistInUploadArea(getValidatedDirectory(false, assemblyName), params.context, assemblyName);
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
			String assemblyName = assemblyInfoEntry.getName();
			getValidatedDirectory(false, assemblyName);
			Submit submit = new Submit(params, assemblyInfoEntry);
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

	private Path getValidatedDirectory(boolean create, String name) throws FtpException {
		Path validatedDirectory = Paths.get(Paths.get(params.manifest).getParent().toString() + File.separator + contextE + File.separator + name);
		try {
			if (create) {
                if (!Files.exists(validatedDirectory))
                    Files.createDirectory(validatedDirectory);
            } else {
                if (!Files.exists(validatedDirectory)) {
                    System.out.println("Directory does not exist, -validate option required." + validatedDirectory);
                    System.exit(3);
                }
                if (Files.list(validatedDirectory).count() == 0) {
                    System.out.println("Directory does not contain any files, -validate option required." + validatedDirectory);
                    System.exit(3);
                }
            }
		} catch (IOException e) {
			throw new FtpException(e.getMessage());
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

	private static ManifestFileReader readAndValidateManifestFile(Params params) {
		ManifestFileReader manifestFileReader = null;
		try {
			File manifestFile = new File(params.manifest);
			String outputDir = params.outputDir == null ? manifestFile.getParent() : params.outputDir;
			Path path = Paths.get(outputDir + File.separator + manifestFile.getName() + ".report");
			if (Files.exists(path))
				Files.delete(path);
			Files.createFile(path);
			File manifestReportF = path.toFile();
			ManifestFileValidator manifestValidator = new ManifestFileValidator();
			ValidationPlanResult result = manifestValidator.validate(manifestFile, params.context);
			if (!result.isValid()) {
                try (Writer writer = new PrintWriter(manifestReportF.getAbsolutePath() + ".report", "UTF-8")) {
                    GenomeAssemblyFileUtils.writeValidationResult(result, writer);
                }
                System.err.println("Manifest file validation failed, please check the report file for errors :" + manifestReportF.getAbsolutePath());
                System.exit(2);
            }
			manifestFileReader = new ManifestFileReader();
			ValidationPlanResult validationPlanResult = manifestFileReader.read(params.manifest);
			if (validationPlanResult != null && validationPlanResult.getMessages(Severity.ERROR) != null && !validationPlanResult.getMessages(Severity.ERROR).isEmpty()) {
                System.err.println("Failed to read manifest file :" + manifestReportF.getAbsolutePath());
                System.exit(2);
            }
		} catch (IOException e) {
			System.err.println("Failed to read manifest file :" + params.manifest);
			System.exit(2);
		}
		return manifestFileReader;
	}

	private AssemblyInfoEntry getAssemblyInfo(String infoFile) {
		AssemblyInfoEntry assemblyInfoEntry = null;
		try {
			Path infoFilePath = Paths.get(infoFile);
			if (!Files.exists(infoFilePath)) {
                System.err.println("Info file " + infoFile + " not found.");
                System.exit(2);
            }
			ValidationResult parseResult = new ValidationResult();
			assemblyInfoEntry =  FileUtils.getAssemblyEntry(infoFilePath.toFile(), parseResult);
			assemblyInfoEntry.setName(assemblyInfoEntry.getName().trim().replaceAll("\\s+", "_"));
		} catch (IOException e) {
			System.err.println("Info file " + infoFile + " not found.");
			System.exit(2);
		}
		return assemblyInfoEntry;
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
