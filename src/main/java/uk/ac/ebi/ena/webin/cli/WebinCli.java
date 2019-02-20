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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.OutputStreamAppender;
import net.sf.cram.ref.PathPattern;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationEngineException.ReportErrorType;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.service.LatestReleaseService;
import uk.ac.ebi.ena.service.LatestReleaseService.GitHubReleaseAsset;
import uk.ac.ebi.ena.service.LatestReleaseService.GitHubReleaseInfo;
import uk.ac.ebi.ena.service.SubmitService;
import uk.ac.ebi.ena.service.VersionService;
import uk.ac.ebi.ena.submit.SubmissionBundle;
import uk.ac.ebi.ena.upload.ASCPService;
import uk.ac.ebi.ena.upload.FtpService;
import uk.ac.ebi.ena.upload.UploadService;
import uk.ac.ebi.ena.version.HotSpotRuntimeVersion;

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

	private static final String INVALID_CONTEXT = "Invalid context: ";
	public static final String MISSING_CONTEXT = "Missing context or unique name.";
	private final static String INVALID_VERSION = "Your current application version webin-cli __VERSION__.jar is out of date, please download the latest version from https://github.com/enasequence/webin-cli/releases.";

	private final static String LOG_FILE_NAME= "webin-cli.report";
    private static final String JAVA_IO_TMPDIR_PROPERTY_NAME = "java.io.tmpdir";
    private static final String USER_HOME_PROPERTY_NAME = "user.home";


	private Params params;
	private WebinCliContext context;

	private static final Logger log = LoggerFactory.getLogger(WebinCli.class);

	public static class
	Params	
	{
		@Parameter()
		private List<String> unrecognisedOptions = new ArrayList<>();

		@Parameter(names = "help")
		public boolean help;
		
		@Parameter(names = ParameterDescriptor.test, description = ParameterDescriptor.testFlagDescription)
		public boolean test;

		@Parameter(names = ParameterDescriptor.context, description = ParameterDescriptor.contextFlagDescription, required = true,validateWith = ContextValidator.class)
		public String context;
		
		@Parameter(names = { ParameterDescriptor.userName, ParameterDescriptor.userNameSynonym }, description = ParameterDescriptor.userNameFlagDescription, required = true)
		public String userName;
		
		@Parameter(names = ParameterDescriptor.password, description = ParameterDescriptor.passwordFlagDescription, required = true)
		public String password;
		
		@Parameter(names = ParameterDescriptor.manifest, description = ParameterDescriptor.manifestFlagDescription, required = true,validateWith = ManifestFileValidator.class)
		public String manifest;
		
		@Parameter(names = { ParameterDescriptor.outputDir, ParameterDescriptor.outputDirSynonym }, description = ParameterDescriptor.outputDirFlagDescription,validateWith = OutputDirValidator.class)
		public String outputDir;
		
		@Parameter(names = ParameterDescriptor.validate, description = ParameterDescriptor.validateFlagDescription)
		public boolean validate;
		
		@Parameter(names = ParameterDescriptor.submit, description = ParameterDescriptor.submitFlagDescription)
		public boolean submit;
		
        @Parameter(names = { ParameterDescriptor.centerName, ParameterDescriptor.centerNameSynonym }, description = ParameterDescriptor.centerNameFlagDescription)
        public String centerName;
        
        @Parameter(names = ParameterDescriptor.version, description = ParameterDescriptor.versionFlagDescription)
        public boolean version;
	
        @Parameter(names = { ParameterDescriptor.inputDir, ParameterDescriptor.inputDirSynonym }, description = ParameterDescriptor.inputDirFlagDescription, hidden=true )
        public String inputDir = ".";
        
        @Parameter(names = ParameterDescriptor.ascp, description = ParameterDescriptor.tryAscpDescription)
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
        System.exit( __main( args ) );
	}

    
    private static int
    __main( String... args )
    {
        ValidationMessage.setDefaultMessageFormatter( ValidationMessage.TEXT_TIME_MESSAGE_FORMATTER_TRAILING_LINE_END );
        ValidationResult.setDefaultMessageFormatter( null );

        try 
        {
            checkRuntimeVersion();
            // checkLatestVersion();
            
            if( args != null && args.length > 0 )
            {
                List<String> found = Arrays.stream( args ).collect( Collectors.toList() );
                if( found.contains( ParameterDescriptor.help ) ) {
                    printUsage();
                    return SUCCESS;
                }
                
                if( found.contains( ParameterDescriptor.version ) )
                {
                    log.info( getFormattedProgramVersion() );
                    return SUCCESS;
                }
            }

            Params params = parseParameters( args );
            if( null == params )
                return USER_ERROR;
            
            if( !params.validate && !params.submit ) 
            {
                log.error("Either -validate or -submit option must be provided.");
                printHelp();
                return USER_ERROR;
            }
            
            checkVersion( params.test );

            WebinCli webinCli = new WebinCli();
            WebinCliParameters parameters = webinCli.init( params );
            webinCli.execute( parameters );
            
            return SUCCESS;
            
        } catch( WebinCliException e ) 
        {
            log.error( e.getMessage() );

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
            log.error( e.getMessage() );

            if(ReportErrorType.SYSTEM_ERROR.equals(e.getErrorType()))
                return SYSTEM_ERROR;
            else
                return USER_ERROR;
            
        } catch( Throwable e ) 
        {
            StringWriter sw = new StringWriter();
            e.printStackTrace( new PrintWriter( sw ) );
            log.error( sw.toString() );
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


	WebinCliParameters
    init( Params params )
    {
        this.params = params;
        this.context = WebinCliContext.valueOf( params.context );

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

        return parameters;
    }

	
	private void 
	initTimedAppender( String name, OutputStreamAppender<ILoggingEvent> appender ) 
	{
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(loggerContext);
		encoder.setPattern( "%d{\"yyyy-MM-dd'T'HH:mm:ss\"} %-5level: %msg%n" );
		encoder.start();
		appender.setName( name );
		appender.setContext( loggerContext );
		appender.setEncoder( encoder );
		appender.start();
	}

	
	private void 
	initTimedFileLogger( WebinCliParameters parameters )
	{
		FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
		String logFile = new File( createOutputDir( parameters, "." ), LOG_FILE_NAME ).getAbsolutePath();
		fileAppender.setFile( logFile );
		fileAppender.setAppend( false );
		initTimedAppender( "FILE", fileAppender );

		log.info( "Creating report file: " + logFile );

		ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger( Logger.ROOT_LOGGER_NAME );
		logger.addAppender( fileAppender );
	}

	/*
	private void initTimedConsoleLogger() {
		ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
		initTimedAppender("CONSOLE", consoleAppender);

		ch.qos.logback.classic.Logger logger =
				(ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.detachAppender("CONSOLE");
		logger.addAppender(consoleAppender);
	}
	*/

	void
	execute(WebinCliParameters parameters) throws Exception
	{
		try {
			context = WebinCliContext.valueOf( params.context );
		} catch (IllegalArgumentException e) {
			throw WebinCliException.createUserError(INVALID_CONTEXT, params.context);
		}

		// initTimedConsoleLogger();
		initTimedFileLogger(parameters);

		AbstractWebinCli<?> validator = context.getValidatorClass().newInstance();
		validator.setTestMode( params.test );
		validator.init( parameters );

		if (params.validate || validator.getSubmissionBundle() == null) {
			doValidation(validator);
		}

		if (params.submit) {
			doSubmit(validator);
		}
	}

	
	private void
	doValidation(AbstractWebinCli<?> validator)
	{
	   try 
	   {
           validator.validate();
           validator.prepareSubmissionBundle();
           log.info( VALIDATE_SUCCESS );
           
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
    doSubmit( AbstractWebinCli<?> validator )
    {
		SubmissionBundle bundle = validator.getSubmissionBundle();

        UploadService ftpService = params.ascp && new ASCPService().isAvaliable() ? new ASCPService() : new FtpService();
        
        try 
        {
            ftpService.connect( params.userName, params.password );
            ftpService.ftpDirectory( bundle.getUploadFileList(), bundle.getUploadDirectory(), validator.getParameters().getInputDir().toPath() );
			log.info( UPLOAD_SUCCESS );

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
            SubmitService submitService = new SubmitService.Builder()
                                                           .setSubmitDir( bundle.getSubmitDirectory().getPath() )
                                                           .setUserName( params.userName )
                                                           .setPassword( params.password )
                                                           .setTest( params.test )
                                                           .build();
            submitService.doSubmission( bundle.getXMLFileList(), bundle.getCenterName(), getFormattedProgramVersion() );

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
				log.error( "Unrecognised options: " + String.join(", ", params.unrecognisedOptions));
				printHelp();
				return null;
			}
			
	        return params;
	        
		} catch( Exception e )
		{
			log.error( e.getMessage() );
			return null;
		}
	}

    private static void 
	printUsage() 
	{
	    log.info( new StringBuilder()
				.append( "Program options: " )
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
		log.info( "Please use " + ParameterDescriptor.help + " to see all command line options." );
	}

	
	private static void 
	writeReturnCodes()	
	{
		HashMap<Integer, String> returnCodeMap = new HashMap<>();
		returnCodeMap.put( SUCCESS, "SUCCESS" );
		returnCodeMap.put( SYSTEM_ERROR, "INTERNAL ERROR" );
		returnCodeMap.put( USER_ERROR, "USER ERROR" );
		returnCodeMap.put( VALIDATION_ERROR, "VALIDATION ERROR" );
		log.info( "Exit codes: " + returnCodeMap.toString() );
	}


	public static class ContextValidator implements IParameterValidator {
		@Override
		public void validate(String name, String value)	throws ParameterException {
			try {
				WebinCliContext.valueOf(value);
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

	public static class ManifestFileValidator implements IParameterValidator {
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
		
		if( !new VersionService.Builder()
		                       .setTest( test )
		                       .build().isVersionValid( version ) )
			throw WebinCliException.createUserError( INVALID_VERSION.replaceAll( "__VERSION__", version ) );
	}

	
	private static void
	checkLatestVersion()
	{
	    String root = System.getProperty( USER_HOME_PROPERTY_NAME );
	    root = null != root ? root : System.getProperty( JAVA_IO_TMPDIR_PROPERTY_NAME );
	    root = null != root ? root : ".";
	    Path lock = Paths.get( root, ".latest-version-check" );

	    try
	    {
	        long current = System.currentTimeMillis();
    	    if( !Files.exists( lock ) || Files.getLastModifiedTime( lock ).toMillis() + 24 * 60 * 60 * 1000 > current )
    	    {
    	        Files.write( lock, String.valueOf( current ).getBytes(), 
    	                     StandardOpenOption.SYNC, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING );
    	        Files.setLastModifiedTime( lock, FileTime.fromMillis( current ) );
    	        
    	        LatestReleaseService lr = new LatestReleaseService.Builder().build();
                GitHubReleaseInfo info = lr.getLatestInfo();
                if( 1 != info.assets.size() )
                {
                    log.debug( "Unable to fetch information about latest release" );
                    return;
                }
                
                GitHubReleaseAsset ra = info.assets.get( 0 );
                
                if( !"uploaded".equals( ra.state ) )
                {
                    log.debug( "Unable to fetch information about latest release"  );
                    return;
                }
                String version = WebinCli.class.getPackage().getImplementationVersion();
                
                if( null == version || ra.created_at.after( new Timestamp( current ) ) )
                    log.info( "New version of WebinCli is available: " + ra.name );
    	    }
	    } catch( WebinCliException | IOException ioe )
	    {
	       log.debug( ioe.getMessage() );
	    }
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
