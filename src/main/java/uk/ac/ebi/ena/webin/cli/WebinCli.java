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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationEngineException.ReportErrorType;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.submit.SubmissionBundle;
import uk.ac.ebi.ena.submit.Submit;
import uk.ac.ebi.ena.upload.ASCPService;
import uk.ac.ebi.ena.upload.FtpService;
import uk.ac.ebi.ena.upload.UploadService;
import uk.ac.ebi.ena.version.HotSpotRuntimeVersion;
import uk.ac.ebi.ena.service.VersionService;
import uk.ac.ebi.ena.version.VersionManager;

// @SpringBootApplication
public class WebinCli { // implements CommandLineRunner
	public final static int SUCCESS = 0;
	public final static int SYSTEM_ERROR = 1;
	public final static int USER_ERROR = 2;
	public final static int VALIDATION_ERROR = 3;

	private static final String VALIDATE_SUCCESS = "The submission has been validated successfully. ";

	private static final String VALIDATE_USER_ERROR = "Submission validation failed because of a user error. " +
			"Please check the report directory for the errors: ";
	private static final String VALIDATE_SYSTEM_ERROR = "Submission validation failed because of a system error. ";

	private static final String UPLOAD_SUCCESS = "The files have been uploaded to webin.ebi.ac.uk. ";

	private static final String UPLOAD_USER_ERROR = "Failed to upload files to webin.ebi.ac.uk because of a user error. ";
	private static final String UPLOAD_SYSTEM_ERROR = "Failed to upload files to webin.ebi.ac.uk because of a system error. ";

	public static final String SUBMIT_SUCCESS = "The submission has been completed successfully. ";

	private static final String SUBMIT_USER_ERROR = "The submission has failed because of a user error. ";
	private static final String SUBMIT_SYSTEM_ERROR = "The submission has failed because of a system error. ";

	public static final String AUTHENTICATION_ERROR = "Invalid submission account user name or password.";

	public static final String INVALID_CONTEXT = "Invalid context: ";
	public static final String MISSING_CONTEXT = "Missing context or unique name.";
	private final static String INVALID_VERSION = "Your current application version webin-cli __VERSION__.jar is out of date, please download the latest version from https://github.com/enasequence/webin-cli/releases.";
    
	
	private Params params;
	private ContextE contextE;
    private AbstractWebinCli<?> validator;

	public static class
	Params	
	{
		@Parameter()
		private List<String> unrecognisedOptions = new ArrayList<>();

		@Parameter(names = "help", required = false)
		public boolean help;
		
		@Parameter(names = ParameterDescriptor.test, description = ParameterDescriptor.testFlagDescription, required = false)
		public boolean test;

		@Parameter(names = ParameterDescriptor.ignoreErrors, description = ParameterDescriptor.ignoreErrorsFlagDescription, required = false)
		public boolean ignoreErrors;

		@Parameter(names = ParameterDescriptor.context, description = ParameterDescriptor.contextFlagDescription, required = true,validateWith = contextValidator.class)
		public String context;
		
		@Parameter(names = { ParameterDescriptor.userName, ParameterDescriptor.userNameSynonym }, description = ParameterDescriptor.userNameFlagDescription, required = true)
		public String userName;
		
		@Parameter(names = ParameterDescriptor.password, description = ParameterDescriptor.passwordFlagDescription, required = true)
		public String password;
		
		@Parameter(names = ParameterDescriptor.manifest, description = ParameterDescriptor.manifestFlagDescription, required = true,validateWith = manifestFileValidator.class)
		public String manifest;
		
		@Parameter(names = { ParameterDescriptor.outputDir, ParameterDescriptor.outputDirSynonym }, description = ParameterDescriptor.outputDirFlagDescription,validateWith = OutputDirValidator.class)
		public String outputDir;
		
		@Parameter(names = ParameterDescriptor.validate, description = ParameterDescriptor.validateFlagDescription, required = false)
		public boolean validate;
		
		@Parameter(names = ParameterDescriptor.submit, description = ParameterDescriptor.submitFlagDescription, required = false)
		public boolean submit;
		
        @Parameter(names = { ParameterDescriptor.centerName, ParameterDescriptor.centerNameSynonym }, description = ParameterDescriptor.centerNameFlagDescription, required = false )
        public String centerName;
        
        @Parameter(names = ParameterDescriptor.version, description = ParameterDescriptor.versionFlagDescription, required = false )
        public boolean version;
	
        @Parameter(names = { ParameterDescriptor.inputDir, ParameterDescriptor.inputDirSynonym }, description = ParameterDescriptor.inputDirFlagDescription, required = false, hidden=true )
        public String inputDir = ".";
        
        @Parameter(names = ParameterDescriptor.ascp, description = ParameterDescriptor.tryAscpDescription, required = false )
        public boolean ascp;
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
        /*
		new SpringApplicationBuilder(WebinCli.class)
				.bannerMode(Banner.Mode.OFF)
				.web(WebApplicationType.NONE)
				.logStartupInfo(false)
				.run(args);
        */

        System.exit( __main( args ));
	}

    public static int
    __main( String... args )
    {
        ValidationMessage.setDefaultMessageFormatter( ValidationMessage.TEXT_TIME_MESSAGE_FORMATTER_TRAILING_LINE_END );
        ValidationResult.setDefaultMessageFormatter( null );

        try 
        {
            checkRuntimeVersion();
            if( args != null && args.length > 0 )
            {
                List<String> found = Arrays.stream( args ).collect( Collectors.toList() );
                if( found.contains( ParameterDescriptor.help ) ) {
                    printUsage();
                    return SUCCESS;
                }
                
                if( found.contains( ParameterDescriptor.version ) )
                {
                    WebinCliReporter.writeToConsole( getFormattedProgramVersion() );
                    return SUCCESS;
                }

                if( found.contains( ParameterDescriptor.latest ) )
                    return VersionManager.launchLatestVersion( found.stream().filter( e -> !e.equals( ParameterDescriptor.latest ) ).toArray( sz -> new String[ sz ] ) );
            }

            Params params = parseParameters( args );
            if( null == params )
                return USER_ERROR;
            
            if( !params.validate && !params.submit ) 
            {
                WebinCliReporter.writeToConsole( Severity.ERROR, "Either -validate or -submit option must be provided.");
                printHelp();
                return USER_ERROR;
            }
            
            checkVersion( params.test );

            WebinCli webinCli = new WebinCli();
            webinCli.init( params );
            webinCli.execute();
            
            return SUCCESS;
            
        } catch( WebinCliException e ) 
        {
            WebinCliReporter.writeToConsole( Severity.ERROR, e.getMessage() );
            
            if( null != WebinCliReporter.getDefaultReport() )
                WebinCliReporter.writeToFile( WebinCliReporter.getDefaultReport(), Severity.ERROR, e.getMessage() );
            
            switch( e.getErrorType() )
            {
                default:
                    return SYSTEM_ERROR;
                    
                case USER_ERROR:
                    return USER_ERROR;
                    
                case VALIDATION_ERROR:
                    return VALIDATION_ERROR;
            }
            
        } catch( ValidationEngineException e ) 
        {
            WebinCliReporter.writeToConsole( Severity.ERROR, e.getMessage() );
            WebinCliReporter.writeToFile( WebinCliReporter.getDefaultReport(), Severity.ERROR, e.getMessage() );
            if(ReportErrorType.SYSTEM_ERROR.equals(e.getErrorType()))
                return SYSTEM_ERROR;
            else
                return USER_ERROR;
            
        } catch( Throwable e ) 
        {
            StringWriter sw = new StringWriter();
            e.printStackTrace( new PrintWriter( sw ) );
            WebinCliReporter.writeToConsole( Severity.ERROR, sw.toString() );
            WebinCliReporter.writeToFile( WebinCliReporter.getDefaultReport(), Severity.ERROR, sw.toString() );
            return SYSTEM_ERROR;
        }
    }
    
    
    private static void 
    checkRuntimeVersion()
    {
        HotSpotRuntimeVersion jrv = new HotSpotRuntimeVersion();
        if( jrv.isHotSpot() && !jrv.isComplient() )
            throw WebinCliException.createSystemError( String.format( "Your current HotSpot(TM) JVM %s is out of date and not supported, please download the latest version from https://java.com. Minimal HotSpot(TM) JVM supported is: %s",
                                                                      String.valueOf( jrv.getCurrentVersion() ),
                                                                      String.valueOf( jrv.getMinVersion() ) ) );
    }


    void 
    init( Params params ) throws Exception
    {
        this.params = params;
        this.contextE = ContextE.valueOf( String.valueOf( params.context ).toLowerCase() );

        params.manifest = getFullPath( params.manifest );
        File manifestFile = new File( params.manifest );

        String outputDir = getFullPath( params.outputDir == null ? manifestFile.getParent() : params.outputDir );

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setManifestFile( manifestFile );
        parameters.setInputDir( Paths.get( params.inputDir ).toAbsolutePath().toFile() );
        parameters.setOutputDir( Paths.get( outputDir ).toAbsolutePath().toFile() );
        parameters.setUsername( params.userName );
        parameters.setPassword( params.password );
        parameters.setCenterName( params.centerName );
        parameters.setTestMode( params.test );

		WebinCliReporter.setDefaultReportDir(createOutputDir(parameters, "."));

		this.validator = contextE.getValidatorClass().newInstance();
		this.validator.setTestMode( params.test );
		this.validator.setIgnoreErrorsMode( params.ignoreErrors );
		this.validator.init( parameters );
    }
    

	void 
	execute()
	{
		try {
			contextE = ContextE.valueOf(params.context);
		} catch (IllegalArgumentException e) {
			throw WebinCliException.createUserError(INVALID_CONTEXT, params.context);
		}

		SubmissionBundle sb = validator.getSubmissionBundle();

		if (params.validate || sb == null) {
			doValidation();
			sb = validator.getSubmissionBundle();
		}

		if (params.submit) {
			doSubmit( sb );
		}
	}

	
	private void 
	doValidation() 
	{
	   try 
	   {
           validator.validate();
           validator.prepareSubmissionBundle();
           WebinCliReporter.writeToConsole( Severity.INFO, VALIDATE_SUCCESS );
           
	   } catch( WebinCliException e ) 
	   {
	      switch( e.getErrorType() ) 
	      { 
	          case USER_ERROR:
	               throw WebinCliException.createUserError( VALIDATE_USER_ERROR, e.getMessage() );
	               
	          case VALIDATION_ERROR:
	               throw WebinCliException.createValidationError( VALIDATE_USER_ERROR, e.getMessage() );
	               
	          case SYSTEM_ERROR:
	               throw WebinCliException.createSystemError( VALIDATE_SYSTEM_ERROR, e.getMessage() );
	      }
	   } catch( Throwable e ) 
	   {
	      throw WebinCliException.createSystemError( VALIDATE_SYSTEM_ERROR, e.getMessage() );
	   }
	}
	 
	
	private void 
    doSubmit( SubmissionBundle bundle )
    {
        UploadService ftpService = params.ascp && new ASCPService().isAvaliable() ? new ASCPService() : new FtpService();
        
        try 
        {
            ftpService.connect( params.userName, params.password );
            ftpService.ftpDirectory( bundle.getUploadFileList(), bundle.getUploadDirectory(), validator.getParameters().getInputDir().toPath() );
			WebinCliReporter.writeToConsole( Severity.INFO, UPLOAD_SUCCESS );

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
            Submit submit = new Submit( params, bundle.getSubmitDirectory().getPath() );
            submit.doSubmission( bundle.getXMLFileList(), bundle.getCenterName(), getFormattedProgramVersion() );

        } catch( WebinCliException e ) 
        {
            e.throwAddMessage( SUBMIT_USER_ERROR, SUBMIT_SYSTEM_ERROR );
        }
    }
	

    private static Params 
    parseParameters( String... args ) 
    {
		Params params = new Params();
		JCommander jCommander = JCommander.newBuilder().expandAtSign( false ).addObject( params ).build();
		try 
		{
			jCommander.parse( args );

			if( !params.unrecognisedOptions.isEmpty() )  
			{
				WebinCliReporter.writeToConsole( Severity.ERROR, "Unrecognised options: " + params.unrecognisedOptions.stream().collect( Collectors.joining(", " ) ) );
				printHelp();
				return null;
			}
			
	        return params;
	        
		} catch( Exception e )
		{
			WebinCliReporter.writeToConsole( Severity.ERROR, e.getMessage() );
			return null;
		}
	}

	
    private static void 
	printUsage() 
	{
	    WebinCliReporter.writeToConsole( new StringBuilder().append( "Program options: " )
	                                                .append( '\n' )
	                                                .append( '\t' )
	                                                .append( "\n" + ParameterDescriptor.context )
	                                                .append( ParameterDescriptor.contextFlagDescription )
	                                                .append( '\n' )
	                                                
	                                                .append( "\n" + ParameterDescriptor.userName )
	                                                //.append( "\n" + ParameterDescriptor.userNameSynonym )
	                                                .append( ParameterDescriptor.userNameFlagDescription )
	                                                .append( '\n' )
	                                                
	                                                .append( "\n" + ParameterDescriptor.password )
	                                                .append( ParameterDescriptor.passwordFlagDescription )
	                                                .append( '\n' )
	                                                
	                                                .append( "\n" + ParameterDescriptor.manifest )
	                                                .append( ParameterDescriptor.manifestFlagDescription )
	                                                .append( '\n' )
	                                                
                                                    .append( "\n" + ParameterDescriptor.inputDir )
                                                    //.append( "\n" + ParameterDescriptor.inputDirSynonym )
                                                    .append( ParameterDescriptor.inputDirFlagDescription )
                                                    .append( '\n' )
	                                                
                                                    .append( "\n" + ParameterDescriptor.outputDir )
	                                                //.append( "\n" + ParameterDescriptor.outputDirSynonym 
                                                    .append( ParameterDescriptor.outputDirFlagDescription )
	                                                .append( '\n' )
	                                                
	                                                .append( "\n" + ParameterDescriptor.validate )
	                                                .append( ParameterDescriptor.validateFlagDescription )
	                                                .append( '\n' )
	                                                
	                                                .append( "\n" + ParameterDescriptor.submit )
	                                                .append( ParameterDescriptor.submitFlagDescription )
	                                                .append( '\n' )
	                                                
	                                                .append( "\n" + ParameterDescriptor.centerName )
	                                                //.append( "\n" + ParameterDescriptor.centerNameSynonym ) 
	                                                .append( ParameterDescriptor.centerNameFlagDescription )
	                                                .append( '\n' )
	                                                
	                                                .append( "\n" + ParameterDescriptor.ascp)
	                                                .append( ParameterDescriptor.tryAscpDescription )
	                                                .append( '\n' )
	                                                
	                                                .append( "\n" + ParameterDescriptor.version )
	                                                .append( ParameterDescriptor.versionFlagDescription )
	                                                .append( '\n' )
/*	                                                
                                                    .append( "\n" + ParameterDescriptor.latest )
                                                    .append( ParameterDescriptor.latestFlagDescription )
                                                    .append( '\n' )
*/
                                                    .append( "\n" + ParameterDescriptor.help )
													.append( ParameterDescriptor.helpFlagDescription )
													.append( '\n' )
													.append( '\t' )
	                                                .append( '\n' ).toString() );
		writeReturnCodes();
	}

	
    private static void 
	printHelp() 
	{
		WebinCliReporter.writeToConsole( Severity.INFO, "Please use " + ParameterDescriptor.help + " to see all command line options." );
	}

	
	private static void 
	writeReturnCodes()	
	{
		HashMap<Integer, String> returnCodeMap = new HashMap<>();
		returnCodeMap.put( SUCCESS, "SUCCESS" );
		returnCodeMap.put( SYSTEM_ERROR, "INTERNAL ERROR" );
		returnCodeMap.put( USER_ERROR, "USER ERROR" );
		returnCodeMap.put( VALIDATION_ERROR, "VALIDATION ERROR" );
		WebinCliReporter.writeToConsole( "Exit codes: " + returnCodeMap.toString() );
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
				throw new ParameterException("The output directory '" + value + "' does not exist.");
		}
	}

	public static class manifestFileValidator implements IParameterValidator {
		@Override
		public void validate(String name, String value)	throws ParameterException {
			File file = new File(value);
			if(!file.exists())
				throw new ParameterException("The manifest file '" + value + "' does not exist.");
		}
	}


	private static void 
	checkVersion( boolean test ) 
	{
		String version = WebinCli.class.getPackage().getImplementationVersion();
		
		if( null == version || version.isEmpty() )
		    return;
		
		VersionService versionService = new VersionService();
		if( !versionService.isVersionValid( version, test ) )
			throw WebinCliException.createUserError( INVALID_VERSION.replaceAll( "__VERSION__", version ) );
	}


	// Directory creation.
	static File
	getReportFile( File dir, String filename, String suffix )
	{
		if( dir == null || !dir.isDirectory() )
			throw WebinCliException.createSystemError( "Invalid report directory: " + filename );

		return new File( dir, Paths.get( filename ).getFileName().toString() + suffix );
	}

	
	public static File
	createOutputDir( WebinCliParameters parameters, String... dirs ) throws WebinCliException
	{
		if (parameters.getOutputDir() == null) 
		{
			throw WebinCliException.createSystemError( "Missing output directory" );
		}

		String[] safeDirs = getSafeOutputDir(dirs);

		Path p;

		try
		{
			p = Paths.get(parameters.getOutputDir().getPath(), safeDirs);
			
		}catch( InvalidPathException ex ) 
		{

			throw WebinCliException.createSystemError( "Unable to create directory: " + ex.getInput() );
		}

		File dir = p.toFile();

		if( !dir.exists() && !dir.mkdirs() )
		{
			throw WebinCliException.createSystemError( "Unable to create directory: " + dir.getPath() );
		}

		return dir;
	}
	

	public static String[]
	getSafeOutputDir( String ... dirs ) 
	{
		return Arrays.stream( dirs )
		             .map( str -> str.replaceAll( "[^a-zA-Z0-9-_\\.]", "_" ) )
		             .map( str -> str.replaceAll( "_+", "_" ) )
		             .map( str -> str.replaceAll( "^_+(?=[^_])", "" ) )
		             .map( str -> str.replaceAll( "(?<=[^_])_+$", "" ) )
		             .toArray( String[]::new );
	}
}
