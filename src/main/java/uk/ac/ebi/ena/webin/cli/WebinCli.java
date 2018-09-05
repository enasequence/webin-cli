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

package uk.ac.ebi.ena.webin.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
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

	public static final String SUBMIT_SUCCESS = "The submission has been completed successfully. ";

	private static final String SUBMIT_USER_ERROR = "The submission has failed because of a user error. " + RESUBMIT_AFTER_USER_ERROR;
	private static final String SUBMIT_SYSTEM_ERROR = "The submission has failed because of a system error. " + RESUBMIT_AFTER_SYSTEM_ERROR;

	public static final String AUTHENTICATION_ERROR = "Invalid submission account user name or password.";

	public static final String INVALID_CONTEXT = "Invalid context: ";
	public static final String MISSING_CONTEXT = "Missing context or unique name.";
	private final static String INVALID_VERSION = "Your current application version webin-cli __VERSION__.jar is out of date, please download the latest version from https://github.com/enasequence/webin-cli/releases.";
	
	private Params params;
	private ContextE contextE;
    private String SUBMIT_DIR = "submit";
    private boolean test_mode;
    private AbstractWebinCli validator;

	private static String outputDir;
	private static String webinCliReportFile;
	private static String infoReportFile;


	public static class 
	Params	
	{
		@Parameter()
		private List<String> unrecognisedOptions = new ArrayList<>();

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

	
	private static String 
	getFormattedProgramVersion()
	{
        String version = WebinCli.class.getPackage().getImplementationVersion();
        return String.format( "%s:%s", WebinCli.class.getSimpleName(), null == version ? "" : version );    
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
                    writeMessageIntoConsole( getFormattedProgramVersion() );
                    System.exit( SUCCESS );
                }

            }

            Params params = parseParameters( args );
            
            if( !params.validate && !params.submit ) 
            {
                printUsageErrorAndExit();
            }

            checkVersion( params.test );
    
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
        } 
        catch( ValidationEngineException e ) 
        {
            writeMessage( Severity.ERROR, e.getMessage() );
            System.exit( SYSTEM_ERROR );
        }
        catch( Throwable e ) 
        {
            StringWriter sw = new StringWriter();
            e.printStackTrace( new PrintWriter( sw ) );
            writeMessage( Severity.ERROR, e.getMessage() );
            System.exit( SYSTEM_ERROR );
        }
    }

  
    void 
    init( Params params ) throws Exception, IOException, FileNotFoundException 
    {
        this.params = params;
        this.contextE = ContextE.valueOf( String.valueOf( params.context ).toLowerCase() );
        this.test_mode = params.test;

        params.manifest = getFullPath( params.manifest );
        File manifestFile = new File( params.manifest );

        outputDir = params.outputDir == null ? manifestFile.getParent() : params.outputDir;
        outputDir = getFullPath( outputDir );

        this.validator = contextE.getValidatorClass().newInstance();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setManifestFile( manifestFile );
        parameters.setInputDir( Paths.get( params.inputDir ).toAbsolutePath().toFile() );
        parameters.setOutputDir( Paths.get( outputDir ).toAbsolutePath().toFile() );
        parameters.setUsername( params.userName );
        parameters.setPassword( params.password );
        parameters.setCenterName( params.centerName );
        parameters.setTestMode( params.test );
        validator.setTestMode( params.test );
		validator.init( parameters );
    }
    

	void 
	execute()
	{
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
	        	throw WebinCliException.createSystemError( "Unable to find attempt of validation. Please re-run validation again." );
		}
	}

	
	private void 
	doValidation() 
	{
		try 
		{
            if( !validator.validate() )
            {
                throw WebinCliException.createValidationError( VALIDATE_USER_ERROR, String.valueOf( validator.getValidationDir() ) );
            }
            validator.prepareSubmissionBundle();
            writeMessage( Severity.INFO, VALIDATE_SUCCESS );
		} catch( IOException /*| NoSuchAlgorithmException */| ValidationEngineException e ) 
		{
			throw WebinCliException.createSystemError(VALIDATE_SYSTEM_ERROR, e.getMessage());
		}
	}


	private void 
    doSubmit( SubmissionBundle bundle )
    {
        UploadService ftpService = params.tryAscp && new ASCPService().isAvaliable() ? new ASCPService() : new FtpService();
        
        try 
        {
            ftpService.connect( params.userName, params.password );
            ftpService.ftpDirectory( bundle.getUploadFileList(), bundle.getUploadDirectory(), validator.getParameters().getInputDir().toPath() );
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
            Submit submit = new Submit( params, bundle.getSubmitDirectory().getPath());
            submit.doSubmission( bundle.getXMLFileList(), bundle.getCenterName(), getFormattedProgramVersion() );

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

			if (params.unrecognisedOptions.size() > 1) {
				writeMessage(Severity.ERROR, "Unrecognised options: " + params.unrecognisedOptions.stream().collect( Collectors.joining(", ")));
				printUsageErrorAndExit();
			}

		} catch (Exception e) {
			writeMessage(Severity.ERROR, e.getMessage());
			printUsageErrorAndExit();
		}
		return params;
	}

	private static void printUsageErrorAndExit() {
		printUsageHelpAndExit();
		System.exit(USER_ERROR);
	}

	private static void printUsageHelpAndExit() {
	    writeMessageIntoConsole( new StringBuilder().append( "Program options: " )
	                                                .append( "\n" + ParameterDescriptor.context + ParameterDescriptor.contextFlagDescription )
	                                                .append( "\n" + ParameterDescriptor.userName + ParameterDescriptor.userNameFlagDescription )
	                                                .append( "\n" + ParameterDescriptor.password + ParameterDescriptor.passwordFlagDescription )
	                                                .append( "\n" + ParameterDescriptor.manifest + ParameterDescriptor.manifestFlagDescription )
                                                    .append( "\n" + ParameterDescriptor.inputDir + ParameterDescriptor.inputDirFlagDescription )
	                                                .append( "\n" + ParameterDescriptor.outputDir + ParameterDescriptor.outputDirFlagDescription )
	                                                .append( "\n" + ParameterDescriptor.validate + ParameterDescriptor.validateFlagDescription )
	                                                .append( "\n" + ParameterDescriptor.submit + ParameterDescriptor.submitFlagDescription )
	                                                .append( "\n" + ParameterDescriptor.centerName + ParameterDescriptor.centerNameFlagDescription )
	                                                .append( "\n" + ParameterDescriptor.tryAscp + ParameterDescriptor.tryAscpDescription )
	                                                .append( "\n" + ParameterDescriptor.version + ParameterDescriptor.versionFlagDescription )
	                                                .append( "\n" ).toString() );
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


	private static void checkVersion( boolean test ) {
		String version = WebinCli.class.getPackage().getImplementationVersion();
		
		if( null == version || version.isEmpty() )
		    return;
		
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

	private static void 
	writeMessageIntoInfoReport(Severity severity, String message) {
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
