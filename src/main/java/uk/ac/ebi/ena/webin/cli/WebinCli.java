/*
 * Copyright 2018-2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.fusesource.jansi.AnsiConsole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.OutputStreamAppender;

import picocli.CommandLine;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationEngineException.ReportErrorType;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.webin.cli.entity.Version;
import uk.ac.ebi.ena.webin.cli.service.SubmitService;
import uk.ac.ebi.ena.webin.cli.service.VersionService;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.upload.ASCPService;
import uk.ac.ebi.ena.webin.cli.upload.FtpService;
import uk.ac.ebi.ena.webin.cli.upload.UploadService;

public class WebinCli {
	public final static int SUCCESS = 0;
	public final static int SYSTEM_ERROR = 1;
	public final static int USER_ERROR = 2;
	public final static int VALIDATION_ERROR = 3;

	private final static String LOG_FILE_NAME= "webin-cli.report";

	private WebinCliCommand params;
	private WebinCliContext context;

	private static final Logger log = LoggerFactory.getLogger(WebinCli.class);

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
            WebinCliCommand params = parseParameters( args );
			if( null == params )
				return USER_ERROR;

            if (params.help || params.version)
				return SUCCESS;
            
            checkVersion( params.test );

            WebinCli webinCli = new WebinCli();
            WebinCliParameters parameters = webinCli.init( params );
            webinCli.execute( parameters );
            
            return SUCCESS;
            
        } catch( WebinCliException e ) 
        {
            log.error( e.getMessage(), e );

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
            log.error( e.getMessage(), e );

            if(ReportErrorType.SYSTEM_ERROR.equals(e.getErrorType()))
                return SYSTEM_ERROR;
            else
                return USER_ERROR;
            
        } catch( Throwable e ) 
        {
            log.error( e.getMessage(), e );
            return SYSTEM_ERROR;
        }
    }


	WebinCliParameters
    init( WebinCliCommand params )
    {
        this.params = params;
        this.context = params.context;

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setManifestFile(  params.manifest );
        parameters.setInputDir( params.inputDir  );
        parameters.setOutputDir( params. outputDir );
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

	void
	execute(WebinCliParameters parameters) throws Exception
	{
		context = params.context;

		// initTimedConsoleLogger();
		initTimedFileLogger(parameters);

		AbstractWebinCli<?> validator = context.getValidatorClass().newInstance();
		validator.setTestMode( params.test );
		validator.readManifest( parameters );

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
           log.info( WebinCliMessage.Cli.VALIDATE_SUCCESS.format() );
           
	   } catch( WebinCliException ex )
	   {
	      switch( ex.getErrorType() )
	      { 
	          case USER_ERROR:
	               throw WebinCliException.userError( ex, WebinCliMessage.Cli.VALIDATE_USER_ERROR.format(ex.getMessage(), validator.getValidationDir()));
	               
	          case VALIDATION_ERROR:
	               throw WebinCliException.validationError( ex, WebinCliMessage.Cli.VALIDATE_USER_ERROR.format(ex.getMessage(), validator.getValidationDir()));
	               
	          case SYSTEM_ERROR:
	               throw WebinCliException.systemError( ex, WebinCliMessage.Cli.VALIDATE_SYSTEM_ERROR.format(ex.getMessage(), validator.getValidationDir()));
	      }
	   } catch( Throwable ex )
	   {
	      throw WebinCliException.systemError( ex, WebinCliMessage.Cli.VALIDATE_SYSTEM_ERROR.format(ex.getMessage(), validator.getValidationDir()));
	   }
	}
	 
	
	private void 
    doSubmit( AbstractWebinCli<?> validator )
    {
		SubmissionBundle bundle = validator.getSubmissionBundle();

        UploadService ftpService = params.ascp && new ASCPService().isAvailable() ? new ASCPService() : new FtpService();
        
        try 
        {
            ftpService.connect( params.userName, params.password );
            ftpService.upload( bundle.getUploadFileList(), bundle.getUploadDirectory(), validator.getParameters().getInputDir().toPath() );
			log.info( WebinCliMessage.Cli.UPLOAD_SUCCESS.format() );

        } catch( WebinCliException e ) 
        {
        	throw WebinCliException.error(e, WebinCliMessage.Cli.UPLOAD_ERROR.format(e.getErrorType().text));
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
            submitService.doSubmission( bundle.getXMLFileList(), bundle.getCenterName(), getVersionForSubmission() );

        } catch( WebinCliException e ) 
        {
			throw WebinCliException.error(e, WebinCliMessage.Cli.SUBMIT_ERROR.format(e.getErrorType().text));
        }
    }
	

    private static WebinCliCommand
    parseParameters( String... args ) 
    {
		AnsiConsole.systemInstall();
		WebinCliCommand params = new WebinCliCommand();
		CommandLine commandLine = new CommandLine(params);
		commandLine.setExpandAtFiles(false);
		commandLine.parse(args);
		
		List<String> jvm_args  = ManagementFactory.getRuntimeMXBean().getInputArguments();
		String classpath = ManagementFactory.getRuntimeMXBean().getClassPath();
		
		commandLine.setCommandName( "java " + ( jvm_args.isEmpty() ? "" : jvm_args.stream().collect( Collectors.joining( " ", "", " " ) ) ) + "-jar " + classpath );
				
		try
		{
			commandLine.parse(args);
			if (commandLine.isUsageHelpRequested()) {
				commandLine.usage(System.out);
				params.help = true;
				return params;
			}

			if (commandLine.isVersionHelpRequested()) {
				commandLine.printVersionHelp(System.out);
				params.version = true;
				return params;
			}

			if (!params.manifest.isFile() || !Files.isReadable(params.manifest.toPath())) {
				log.error("Unable to read the manifest file.");
				printHelp();
				return null;
			}
			params.manifest = params.manifest.getAbsoluteFile();

			if (params.inputDir == null) {
				params.inputDir = Paths.get(".").toFile().getAbsoluteFile();
			}
			params.inputDir = params.inputDir.getAbsoluteFile();

			if (params.outputDir == null) {
				params.outputDir = params.manifest.getParentFile();
			}
			params.outputDir = params.outputDir.getAbsoluteFile();

			if (!params.inputDir.canRead()) {
				log.error("Unable to read from the input directory: " + params.inputDir.getAbsolutePath());
				printHelp();
				return null;
			}

			if (!params.outputDir.canWrite()) {
				log.error("Unable to write to the output directory: " + params.outputDir.getAbsolutePath());
				printHelp();
				return null;
			}

			if( !params.validate && !params.submit ) {
				log.error("Either -validate or -submit option must be provided.");
				printHelp();
				return null;
			}
			
	        return params;
	        
		} catch( Exception e )
		{
			log.error( e.getMessage(), e );
			printHelp();
			return null;
		}
	}

    private static void 
	printHelp() 
	{
		log.info( "Please use " + WebinCliCommand.Options.help + " option to see all command line options." );
	}

	
    public static String 
	getVersionForSubmission()
	{
		String version = getVersion();
		return String.format( "%s:%s", WebinCli.class.getSimpleName(), null == version ? "" : version );
	}


	public static String 
	getVersionForUsage() 
	{
		String version = getVersion();
		return String.format( "%s", null == version ? "no version declared" : version );
	}

	
	private static String 
	getVersion() 
	{
		return WebinCli.class.getPackage().getImplementationVersion();
	}

	
	private static void checkVersion( boolean test )
	{
		String currentVersion = getVersion();
		
		if( null == currentVersion || currentVersion.isEmpty() )
		    return;

		Version version = new VersionService.Builder()
				.setTest( test )
				.build().getVersion( currentVersion );

		log.info(WebinCliMessage.Cli.CURRENT_VERSION.format(currentVersion));

		if (!version.valid) {
			throw WebinCliException.userError(WebinCliMessage.Cli.UNSUPPORTED_VERSION.format(
					version.minVersion,
					version.latestVersion));
		}

		if (version.expire) {
			log.info(WebinCliMessage.Cli.EXPIRYING_VERSION.format(
					new SimpleDateFormat("dd MMM yyyy").format(version.nextMinVersionDate),
					version.nextMinVersion,
					version.latestVersion));
		}
		else if (version.update) {
			log.info(WebinCliMessage.Cli.NEW_VERSION.format(version.latestVersion));
		}

		if (version.comment != null) {
			log.info(version.comment);
		}
	}

	// Directory creation.
	static File
	getReportFile( File dir, String filename, String suffix )
	{
		if( dir == null || !dir.isDirectory() )
			throw WebinCliException.systemError( WebinCliMessage.Cli.INVALID_REPORT_DIR_ERROR.format(filename ));

		return new File( dir, Paths.get( filename ).getFileName().toString() + suffix );
	}

	
	public static File
	createOutputDir( WebinCliParameters parameters, String... dirs ) throws WebinCliException
	{
		if (parameters.getOutputDir() == null) {
			throw WebinCliException.systemError( WebinCliMessage.Cli.MISSING_OUTPUT_DIR_ERROR.format());
		}

		String[] safeDirs = getSafeOutputDir(dirs);

		Path p;

		try {
			p = Paths.get(parameters.getOutputDir().getPath(), safeDirs);
		} catch (InvalidPathException ex) {
			throw WebinCliException.systemError( WebinCliMessage.Cli.CREATE_DIR_ERROR.format(ex.getInput()));
		}

		File dir = p.toFile();

		if (!dir.exists() && !dir.mkdirs()) {
			throw WebinCliException.systemError( WebinCliMessage.Cli.CREATE_DIR_ERROR.format(dir.getPath()));
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
