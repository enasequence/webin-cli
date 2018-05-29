package uk.ac.ebi.ena.webin.cli;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.assembly.InfoFileValidator;
import uk.ac.ebi.ena.assembly.SequenceAssemblyWebinCli;
import uk.ac.ebi.ena.assembly.TranscriptomeAssemblyWebinCli;
import uk.ac.ebi.ena.manifest.ManifestFileValidator;
import uk.ac.ebi.ena.manifest.ManifestFileWriter;
import uk.ac.ebi.ena.manifest.ManifestObj;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.submit.SubmissionBundle;
import uk.ac.ebi.ena.submit.Submit;
import uk.ac.ebi.ena.upload.ASCPService;
import uk.ac.ebi.ena.upload.FtpService;
import uk.ac.ebi.ena.upload.UploadService;
import uk.ac.ebi.ena.utils.FileUtils;
import uk.ac.ebi.ena.version.Version;

public class WebinCli {
	public final static int SUCCESS = 0;
	public final static int SYSTEM_ERROR = 1;
	public final static int USER_ERROR = 2;
	public final static int VALIDATION_ERROR = 3;

	private static final String RESUBMIT_AFTER_USER_ERROR = "Please correct the errors and use the -submit option again.";
	private static final String RESUBMIT_AFTER_SYSTEM_ERROR = "Please use the -submit option again later.";

	private static final String VALIDATE_SUCCESS = "The submission has been validated successfully. ";

	private static final String VALIDATE_USER_ERROR = "Submission validation failed because of a user error. " +
			"Please check the report directory for the errors: ";
	private static final String VALIDATE_SYSTEM_ERROR = "Submission validation failed because of a system error. ";

	private static final String UPLOAD_SUCCESS = "The files have been uploaded to webin.ebi.ac.uk. ";

	private static final String UPLOAD_USER_ERROR = "Failed to upload files to webin.ebi.ac.uk because of a user error. " + RESUBMIT_AFTER_USER_ERROR;
	private static final String UPLOAD_SYSTEM_ERROR = "Failed to upload files to webin.ebi.ac.uk because of a system error. " + RESUBMIT_AFTER_SYSTEM_ERROR;

	/*
	private static final String UPLOAD_CHECK_USER_ERROR = "Failed to check if files have been uploaded to webin.ebi.ac.uk because of a user error. " + RESUBMIT_AFTER_USER_ERROR;
	private static final String UPLOAD_CHECK_SYSTEM_ERROR = "Failed to check if files have been uploaded to webin.ebi.ac.uk because of a system error. " + RESUBMIT_AFTER_SYSTEM_ERROR;
	*/

	public static final String SUBMIT_SUCCESS = "The submission has been completed successfully. ";

	private static final String SUBMIT_USER_ERROR = "The submission has failed because of a user error. " + RESUBMIT_AFTER_USER_ERROR;
	private static final String SUBMIT_SYSTEM_ERROR = "The submission has failed because of a system error. " + RESUBMIT_AFTER_SYSTEM_ERROR;

	public static final String AUTHENTICATION_ERROR = "Invalid submission account user name or password.";

	public static final String INVALID_CONTEXT = "Invalid context: ";
	public static final String MISSING_CONTEXT = "Missing context or unique name.";
	private final static String INVALID_VERSION = "Your current application version webin-cli __VERSION__.jar is out of date, please download the latest version from https://github.com/enasequence/webin-cli/releases.";
	private final static String INVALID_MANIFEST = "Manifest file validation failed. Please check the report file for errors: ";
	@Deprecated private final static String INVALID_INFO = "Info file validation failed. Please check the report file for errors: ";
	
	private Params params;
	private ContextE contextE;
	private final static String VALIDATE_DIR = "validate";
    private String SUBMIT_DIR = "submit";
    private boolean test_mode;
    private AbstractWebinCli validator;
	private final static String INFO_FIELD = "INFO";
	private final static String NAME_FIELD = "ASSEMBLYNAME";

	private static ManifestFileValidator manifestValidator = null;
	private static InfoFileValidator infoValidator = null;

	private static String outputDir;
	private static String reportDir;
	private static String webinCliReportFile;
	private static String infoReportFile;


	public static class 
	Params	
	{
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
		
		@Parameter(names = ParameterDescriptor.submit, description = ParameterDescriptor.submitFlagDescription, required = false)
		public boolean submit;
		
        @Parameter(names = ParameterDescriptor.centerName, description = ParameterDescriptor.centerNameFlagDescription, required = false )
        public String centerName;
        
        @Parameter(names = ParameterDescriptor.version, description = ParameterDescriptor.versionFlagDescription, required = false )
        public boolean version;
	
        @Parameter(names = ParameterDescriptor.inputDir, description = ParameterDescriptor.inputDirFlagDescription, required = false, hidden=true )
        public String inputDir = ".";
        
        @Parameter(names = ParameterDescriptor.tryAscp, description = ParameterDescriptor.tryAscpDescription, required = false )
        public boolean tryAscp;
	}


	private static String getFullPath(String path) {
		return FileSystems.getDefault().getPath(path).normalize().toAbsolutePath().toString();
	}

	
    public static void 
    main( String... args )
    {
        ValidationMessage.setDefaultMessageFormatter( ValidationMessage.TEXT_TIME_MESSAGE_FORMATTER_TRAILING_LINE_END );
        ValidationResult.setDefaultMessageFormatter( null );
        
        try 
        {
            if( args != null && args.length > 0 )
            {
                Set<String> found = Arrays.stream( args ).collect( Collectors.toSet() );

                if( found.contains( "-help" ) )
                    printUsageHelpAndExit();
                
                if( found.contains( "-version" ) )
                {
                    String version = WebinCli.class.getPackage().getImplementationVersion();
                    writeMessageIntoConsole( String.format( "%s, version %s\n", WebinCli.class.getSimpleName(), version ) );
                    System.exit( SUCCESS );
                }

            }

            Params params = parseParameters( args );
            
            if( !params.validate && !params.submit ) 
            {
                printUsageErrorAndExit();
            }

//            checkVersion( params.test );
    
            WebinCli webinCli = new WebinCli();
            webinCli.init( params );
            webinCli.execute();
            
            System.exit( SUCCESS );
            
        } catch( WebinCliException e ) 
        {
            writeMessage( Severity.ERROR, e.getMessage() );
            
            switch( e.getErrorType() )
            {
                case SYSTEM_ERROR:
                    System.exit( SYSTEM_ERROR );
                    
                case USER_ERROR:
                    System.exit( USER_ERROR );
                    
                case VALIDATION_ERROR:
                    System.exit( VALIDATION_ERROR );
            }
        } catch( Throwable e ) 
        {
            StringWriter sw = new StringWriter();
            e.printStackTrace( new PrintWriter( sw ) );
            writeMessage( Severity.ERROR, sw.toString() );
            System.exit( SYSTEM_ERROR );
        }
    }

  
    private void 
    init( Params params ) throws Exception, IOException, FileNotFoundException, ValidationEngineException 
    {
        this.params = params;
        this.contextE = ContextE.valueOf( String.valueOf( params.context ).toLowerCase() );
        this.test_mode = params.test;
        
        params.manifest = getFullPath( params.manifest );
        File manifestFile = new File( params.manifest );

        outputDir = params.outputDir == null ? manifestFile.getParent() : params.outputDir;
        outputDir = getFullPath( outputDir );
        
        //TODO remove
        if( contextE != ContextE.reads )
        {
            String name = peekInfoFileForName(peekManifestForInfoFile(params.manifest));
            createValidateDir( params.context, name );
            createWebinCliReportFile();
        }
        
        infoValidator = new InfoFileValidator();
        manifestValidator = new ManifestFileValidator();
        //TODO remove
        if( contextE != ContextE.reads )
        {
            if (!manifestValidator.validate(manifestFile, reportDir, params.context))
                throw WebinCliException.createUserError(INVALID_MANIFEST, manifestValidator.getReportFile().getAbsolutePath());
            if (!infoValidator.validate(manifestValidator.getReader(), reportDir, params.context))
                throw WebinCliException.createUserError( INVALID_INFO, infoValidator.getReportFile().getAbsolutePath() );
            infoReportFile = infoValidator.getReportFile().getAbsolutePath();
        }

        this.validator = contextE.getValidatorClass().newInstance();
        
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setManifestFile( manifestFile );
        parameters.setInputDir( new File( params.inputDir ) );
        parameters.setOutputDir( new File( outputDir ) );
        parameters.setUsername( params.userName );
        parameters.setPassword( params.password );
        parameters.setCenterName( params.centerName );
        validator.init( parameters );
    }
    

	private void execute() {
		try {
			contextE = ContextE.valueOf(params.context);
		} catch (IllegalArgumentException e) {
			throw WebinCliException.createUserError(INVALID_CONTEXT, params.context);
		}
		
		if (params.validate)
			doValidation();
		
		if( params.submit )
		{
		    SubmissionBundle sb = null;
		    try
		    {
		        sb = validator.getSubmissionBundle();
		    }catch( WebinCliException e )
		    {
		        writeMessageIntoInfoReport( Severity.WARNING, e.getMessage() );
		        throw WebinCliException.createUserError( "Unable to read previous attempt of validation. Please re-run validation again." );
		    }
		    
	        if( null != sb )
	            doSubmit( sb );
	        else
	            doSubmit();
		}
	}

	@Deprecated private Study getStudy() {
		Study study = new Study();
		try {
			study.getStudy(infoValidator.getentry().getStudyId(), params.userName, params.password, params.test);
		}
		catch (WebinCliException e) {
			writeMessageIntoInfoReport(Severity.ERROR, e.getMessage());
			throw e;
		}
		return study;
	}

	@Deprecated private Sample getSample() {
		Sample sample = new Sample();
		try {
			sample.getSample(infoValidator.getentry().getSampleId(), params.userName, params.password, params.test);
		}
		catch (WebinCliException e) {
			writeMessageIntoInfoReport(Severity.ERROR, e.getMessage());
			throw e;
		}
		return sample;
	}

	
	private void 
	doValidation() 
	{
		switch(contextE) {
			case transcriptome:
			{
				TranscriptomeAssemblyWebinCli v = new TranscriptomeAssemblyWebinCli( manifestValidator.getReader(), getSample(), getStudy() );
                v.setReportsDir( reportDir );
				validator = v;
				break;
			}
			case sequence:
			{
				SequenceAssemblyWebinCli v = new SequenceAssemblyWebinCli( manifestValidator.getReader(), getStudy() );
                v.setReportsDir( reportDir );
				validator = v;
				break;
			}	
		}
		
		try 
		{
            if( !validator.validate() )
            {
                throw WebinCliException.createValidationError( VALIDATE_USER_ERROR, reportDir );
            }
            
            //TODO remove
            if( contextE != ContextE.reads )
            {
                String assemblyName = infoValidator.getentry().getName().trim().replaceAll("\\s+", "_");
                File submitDirectory = createSubmitDirectory( assemblyName );
                // Gzip the files validated directory.
    			for( ManifestObj manifestObj: manifestValidator.getReader().getManifestFileObjects() )
                    FileUtils.gZipFile( Paths.get( manifestObj.getFileName() ).toFile() );
    			// Create the manifest in the submit directory.
    			new ManifestFileWriter().write( new File( submitDirectory.getAbsolutePath() + File.separator + assemblyName + ".manifest"), manifestValidator.getReader().getManifestFileObjects() );
            }		
            
            validator.prepareSubmissionBundle();
            writeMessage( Severity.INFO, VALIDATE_SUCCESS );
		} catch( IOException | NoSuchAlgorithmException | ValidationEngineException e ) 
		{
			throw WebinCliException.createSystemError(VALIDATE_SYSTEM_ERROR, e.getMessage());
		}
	}

	private void doFtpUpload(String assemblyName, List<File> uploadFileList) {
	    /*
		if (doFilesExistInUploadArea(uploadFileList, assemblyName))
		    return;
		*/
		FtpService ftpService = new FtpService();
		try {
			ftpService.connect(params.userName, params.password);
			ftpService.ftpDirectory(uploadFileList, params.context, assemblyName);
			writeMessage(Severity.INFO, UPLOAD_SUCCESS);
		} catch (WebinCliException e) {
			e.throwAddMessage(UPLOAD_USER_ERROR, UPLOAD_SYSTEM_ERROR);
		} finally {
			ftpService.disconnect();
		}
	}

    private List<File> getFilesToUpload(String submitDirectory, String assemblyName ) {
        List<File> uploadFilesList = new ArrayList<>();
        for (ManifestObj manifestObj: manifestValidator.getReader().getManifestFileObjects())
            uploadFilesList.add(Paths.get(manifestObj.getFileName()).toFile());
        boolean SUBMITTER_FILES_EXIST = true;
        String missingSubmitterFile = "";
        for (File file: uploadFilesList) {
            if (!file.exists()) {
                missingSubmitterFile += file.getName() + ", ";
                SUBMITTER_FILES_EXIST = false;
            }
        }
        if (!SUBMITTER_FILES_EXIST)
            throw WebinCliException.createUserError("File(s) " + missingSubmitterFile + " do not exist. Please supply file that you would like to submit." );
        File fileManifect = new File(submitDirectory + File.separator + assemblyName + ".manifest");
        File fileManifectMD5 = new File(submitDirectory + File.separator + assemblyName + ".manifest.md5");
        if (!fileManifect.exists() || !fileManifectMD5.exists())
            return null;
        uploadFilesList.add(fileManifect);
        uploadFilesList.add(fileManifectMD5);
        return uploadFilesList;
    }


    @Deprecated
	private static void createValidateDir(String context, String name) throws Exception {
		File reportDirectory = new File(outputDir + File.separator + context + File.separator + name + File.separator + VALIDATE_DIR);
		if (reportDirectory.exists())
			FileUtils.emptyDirectory(reportDirectory);
		else if (!reportDirectory.mkdirs()) {
            throw WebinCliException.createSystemError("Unable to create directory: " + reportDirectory.getPath());
		}
		reportDir = reportDirectory.getAbsolutePath();
	}

    @Deprecated
	private static void createWebinCliReportFile() throws IOException {
		Path reportPath = Paths.get(reportDir + File.separator + "webin-cli.report");
		if (Files.exists(reportPath))
			Files.delete(reportPath);
		Files.createFile(reportPath);
		webinCliReportFile = reportPath.toFile().getAbsolutePath();
	}

	@Deprecated private void doSubmit() {
	    try {
            String assemblyName = infoValidator.getentry().getName().trim().replaceAll("\\s+", "_");
            String submitDirectory = getSubmitDirectory(assemblyName);
            if (submitDirectory == null) {
                doValidation();
                submitDirectory = getSubmitDirectory(assemblyName);
            }
            List<File> uploadFileList = getFilesToUpload(submitDirectory, assemblyName);
            if (uploadFileList == null || uploadFileList.isEmpty()) {
                doValidation();
                uploadFileList = getFilesToUpload(submitDirectory, assemblyName);
            }
		    doFtpUpload(assemblyName, uploadFileList);
			Submit submit = new Submit(params, submitDirectory, infoValidator.getentry());
			submit.doSubmission();
		} catch (WebinCliException e) {
			e.throwAddMessage(SUBMIT_USER_ERROR, SUBMIT_SYSTEM_ERROR);
		}
	}

    private void 
    doSubmit( SubmissionBundle bundle )
    {
        UploadService ftpService = params.tryAscp && new ASCPService().isAvaliable() ? new ASCPService() : new FtpService();
        
        try 
        {
            ftpService.connect( params.userName, params.password );
            ftpService.ftpDirectory( bundle.getUploadFileList(), bundle.getUploadDirectory(), Paths.get( "." ) );
            writeMessage( Severity.INFO, UPLOAD_SUCCESS );
            
        } catch( WebinCliException e ) 
        {
            e.throwAddMessage( UPLOAD_USER_ERROR, UPLOAD_SYSTEM_ERROR );
        } finally 
        {
            ftpService.disconnect();
        }

        try 
        {
            AssemblyInfoEntry aie = new AssemblyInfoEntry();
            aie.setName( "NAME" );
            Submit submit = new Submit( params, bundle.getSubmitDirectory().getPath(), aie );
            submit.doSubmission( bundle.getXMLFileList(), bundle.getCenterName() );

        } catch( WebinCliException e ) 
        {
            e.throwAddMessage( SUBMIT_USER_ERROR, SUBMIT_SYSTEM_ERROR );
        }
    }
	
	
	private File createSubmitDirectory(String assemblyName) {
        File submitDirectory =new File(new File(outputDir) + File.separator + contextE + File.separator + assemblyName + File.separator + SUBMIT_DIR);
        if (submitDirectory.exists())
            FileUtils.emptyDirectory(submitDirectory);
        else
            submitDirectory.mkdirs();
        return submitDirectory;
    }

	private String getSubmitDirectory(String name) {
		File submitDirectory =new File(new File(outputDir) + File.separator + contextE + File.separator + name + File.separator + SUBMIT_DIR);
        if (submitDirectory.exists())
            return submitDirectory.getAbsolutePath();
        else
            return null;
	}

    private static Params parseParameters(String... args) {
		Params params = new Params();
		JCommander jCommander = new JCommander(params);
		try {
			jCommander.parse(args);
		} catch (Exception e) {
			writeMessage(Severity.ERROR, e.getMessage());
			printUsageErrorAndExit();
		}
		return params;
	}

	private static void printUsageErrorAndExit() {
		StringBuilder usage = new StringBuilder();
		//usage.append("The following options are required:\n\t[-context], [-userName], [-password], [-manifest]");
		usage.append("In addition, one of the following options must be provided: [-validate], [-submit]");
		usage.append("\nFor full help please use the option: [-help]");
		writeMessage(Severity.INFO, usage.toString());
		System.exit(USER_ERROR);
	}

	private static void printUsageHelpAndExit() {
	    writeMessageIntoConsole( new StringBuilder().append( "Options: " )
	                                                .append( "\n" + ParameterDescriptor.context + ParameterDescriptor.contextFlagDescription )
	                                                .append( "\n" + ParameterDescriptor.userName + ParameterDescriptor.userNameFlagDescription )
	                                                .append( "\n" + ParameterDescriptor.password + ParameterDescriptor.passwordFlagDescription )
	                                                .append( "\n" + ParameterDescriptor.manifest + ParameterDescriptor.manifestFlagDescription )
	                                                .append( "\n" + ParameterDescriptor.outputDir + ParameterDescriptor.outputDirFlagDescription )
	                                                .append( "\n" + ParameterDescriptor.validate + ParameterDescriptor.validateFlagDescription )
	                                                .append( "\n" + ParameterDescriptor.submit + ParameterDescriptor.submitFlagDescription )
	                                                .append( "\n" + ParameterDescriptor.centerName + ParameterDescriptor.centerNameFlagDescription )
	                                                .append( "\n" + ParameterDescriptor.tryAscp + ParameterDescriptor.tryAscpDescription )
	                                                .append( "\n" + ParameterDescriptor.version + ParameterDescriptor.versionFlagDescription )
	                                                .append( "\n" ).toString() );
//		usage.append( "\n" + ParameterDescriptor.inputDir + ParameterDescriptor.inputDirFlagDescription );
		writeReturnCodes();
		System.exit( SUCCESS );
	}

	private static void writeReturnCodes()	{
		HashMap<Integer, String> returnCodeMap = new HashMap<>();
		returnCodeMap.put(SUCCESS, "SUCCESS");
		returnCodeMap.put(SYSTEM_ERROR, "INTERNAL ERROR");
		returnCodeMap.put(USER_ERROR, "USER ERROR");
		returnCodeMap.put(VALIDATION_ERROR, "VALIDATION ERROR");
		writeMessageIntoConsole( "Exit codes: " + returnCodeMap.toString() + "\n" );
	}

	public static class contextValidator implements IParameterValidator {
		@Override
		public void validate(String name, String value)	throws ParameterException {
			try {
				ContextE.valueOf(value);
			} catch (IllegalArgumentException e) {
				throw new ParameterException(INVALID_CONTEXT + value);
			}
		}
	}
	
	public static class OutputDirValidator implements IParameterValidator {
		@Override
		public void validate(String name, String value)	throws ParameterException {
			File file = new File(value);
			if(!file.exists())
				throw new ParameterException("The output directory '" + value + "' does not exist. Please provide a valid -outputDir option.");
		}
	}

	public static class manifestFileValidator implements IParameterValidator {
		@Override
		public void validate(String name, String value)	throws ParameterException {
			File file = new File(value);
			if(!file.exists())
				throw new ParameterException("The manifest file '" + value + "' does not exist. Please provide a valid -manifest option.");
		}
	}

	private static String peekManifestForInfoFile(String manifest) throws Exception {
		try (Stream<String> stream = Files.lines(Paths.get(manifest))) {
			Optional<String> optional = stream.filter(line -> line.toUpperCase().startsWith(INFO_FIELD))
					.findFirst();
			if (optional.isPresent())
				return optional.get().substring(INFO_FIELD.length()).trim();
			else
				throw WebinCliException.createUserError("Manifest file " + manifest + " is missing the " + INFO_FIELD + " field.");
		}
	}

	@Deprecated public static String peekInfoFileForName(String info) throws Exception {
		try( InputStream fileIs = Files.newInputStream(Paths.get(info) );
		     BufferedInputStream bufferedIs = new BufferedInputStream(fileIs);
		     GZIPInputStream gzipIs = new GZIPInputStream(bufferedIs);
		     BufferedReader reader = new BufferedReader(new InputStreamReader(gzipIs) ); )
		{
    		Optional<String> optional = reader.lines().filter(line -> line.toUpperCase().startsWith(NAME_FIELD))
    				.findFirst();
    		if (optional.isPresent())
    			return optional.get().substring(NAME_FIELD.length()).trim().replaceAll("\\s+", "_");
    		else
    			throw WebinCliException.createUserError("Info file " + info + " is missing the " + NAME_FIELD + " field.");
		} catch( NoSuchFileException no )
		{
		    throw WebinCliException.createUserError( String.format( "%s %s", "Unable to locate file", info ) );
		} catch( ZipException ze )
		{
		    throw WebinCliException.createUserError( String.format( "%s %s", ze.getMessage(), info ) );
		}
	}

	private static void checkVersion( boolean test ) {
		String version = WebinCli.class.getPackage().getImplementationVersion();
		Version versionService = new Version();
		if (!versionService.isVersionValid(version, test ))
			throw WebinCliException.createUserError(INVALID_VERSION.replaceAll("__VERSION__", version));
	}

	/**
	 * Writes messages to the webin cli report file and to the console. If there is no
	 * webin-cli.report file then writes only to the console.
	 */
	public static void 
	writeMessage( Severity severity, String message ) 
	{
		if( message == null || message.isEmpty() )
			return;
		
		String msg = FileUtils.formatMessage( severity, message );	
		
		writeMessageIntoConsole( msg );
		
		if( webinCliReportFile != null )
		{
			try
			{
				Files.write( Paths.get(webinCliReportFile), msg.getBytes( StandardCharsets.UTF_8 ), StandardOpenOption.APPEND, StandardOpenOption.SYNC, StandardOpenOption.CREATE  );
			} catch( IOException e ) 
			{

			}
		}
	}

	public static void writeMessageIntoInfoReport(Severity severity, String message) {
        if (message == null || message.isEmpty())
            return;
        if (infoReportFile != null) {
            try {
                String msg = FileUtils.formatMessage( severity, message );
                Files.write(Paths.get(infoReportFile), msg.getBytes( StandardCharsets.UTF_8 ), StandardOpenOption.APPEND, StandardOpenOption.SYNC, StandardOpenOption.CREATE );
            } catch (IOException e) {}
        }
    }

	
	
	/**
	 * Writes messages to the console.
	 */
	private static void 
	writeMessageIntoConsole( String message ) 
	{
		System.out.print( message );
	}

    
	public boolean
    getTestMode()
    {
        return test_mode;
    }

    
    public void
    setTestMode( boolean test_mode )
    {
        this.test_mode = test_mode;
    }

    
    public ContextE
    getContext()
    {
        return contextE;
    }

    public void
    setContext( ContextE contextE )
    {
        this.contextE = contextE;
    }

}
