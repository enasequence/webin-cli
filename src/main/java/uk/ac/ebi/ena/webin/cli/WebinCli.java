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

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
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

import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationEngineException.ReportErrorType;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.webin.cli.entity.Version;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestSource;
import uk.ac.ebi.ena.webin.cli.service.SubmitService;
import uk.ac.ebi.ena.webin.cli.service.VersionService;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.upload.ASCPService;
import uk.ac.ebi.ena.webin.cli.upload.FtpService;
import uk.ac.ebi.ena.webin.cli.upload.UploadService;

// @SpringBootApplication
public class WebinCli { // implements CommandLineRunner
	public final static int SUCCESS = 0;
	public final static int SYSTEM_ERROR = 1;
	public final static int USER_ERROR = 2;
	public final static int VALIDATION_ERROR = 3;

	private final static String LOG_FILE_NAME= "webin-cli.report";

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
            
        } catch( Throwable e )
        {
            log.error( e.getMessage(), e );
            return SYSTEM_ERROR;
        }
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

	void
	execute(WebinCliParameters parameters)  {
		try {
			context = WebinCliContext.valueOf(params.context);
		} catch (IllegalArgumentException e) {
			throw WebinCliException.userError(WebinCliMessage.Cli.INVALID_CONTEXT_ERROR.format(params.context));
		}

		// initTimedConsoleLogger();
		initTimedFileLogger(parameters);

		File file = parameters.getManifestFile();

		if (file.getName().endsWith(".xls") || file.getName().endsWith(".xlsx")) {
			try {
				DataFormatter dataFormatter = new DataFormatter();
				Workbook workbook = WorkbookFactory.create(file);
				Sheet dataSheet = workbook.getSheetAt(0);
				for (int dataSheetRowNumber = 1 ; ; dataSheetRowNumber++) {
					Row dataRow = dataSheet.getRow(dataSheetRowNumber);
					boolean emptyRow = true;
					for (int columnNumber = 0; columnNumber < ManifestReader.MAX_SPREADSHEET_COLUMNS; columnNumber++) {
						String value = dataFormatter.formatCellValue(dataRow.getCell(columnNumber));
						if (value != null && value.isEmpty()) {
							emptyRow = false;
							break;
						}
						if (emptyRow) {
							break;
						}
						executeInternal(parameters, new ManifestSource(parameters.getManifestFile(), dataSheet, dataSheetRowNumber) );
					}
				}
			}
			catch(Exception ex){
				throw WebinCliException.userError("Unable to read spreadsheet: " + file.getName());
			}
		}
		else {
			executeInternal(parameters, new ManifestSource(parameters.getManifestFile()) );
		}
	}

	private void executeInternal(WebinCliParameters parameters, ManifestSource manifestSource)
	{
		AbstractWebinCli<?> validator = null;
		try {
			validator = context.getValidatorClass().newInstance();
		}
		catch (Exception ex) {
			throw WebinCliException.userError("Unable to create validator");
		}

		validator.setTestMode(params.test);
		validator.init( parameters, manifestSource );

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
            submitService.doSubmission( bundle.getXMLFileList(), bundle.getCenterName(), getFormattedProgramVersion() );

        } catch( WebinCliException e ) 
        {
			throw WebinCliException.error(e, WebinCliMessage.Cli.SUBMIT_ERROR.format(e.getErrorType().text));
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
				.append( ParameterDescriptor.userNameFlagDescription )
				.append( '\n' )

				.append( "\n" + ParameterDescriptor.password )
				.append( ParameterDescriptor.passwordFlagDescription )
				.append( '\n' )

				.append( "\n" + ParameterDescriptor.manifest )
				.append( ParameterDescriptor.manifestFlagDescription )
				.append( '\n' )

				.append( "\n" + ParameterDescriptor.inputDir )
				.append( ParameterDescriptor.inputDirFlagDescription )
				.append( '\n' )

				.append( "\n" + ParameterDescriptor.outputDir )
				.append( ParameterDescriptor.outputDirFlagDescription )
				.append( '\n' )

				.append( "\n" + ParameterDescriptor.validate )
				.append( ParameterDescriptor.validateFlagDescription )
				.append( '\n' )

				.append( "\n" + ParameterDescriptor.submit )
				.append( ParameterDescriptor.submitFlagDescription )
				.append( '\n' )

				.append( "\n" + ParameterDescriptor.centerName )
				.append( ParameterDescriptor.centerNameFlagDescription )
				.append( '\n' )

				.append( "\n" + ParameterDescriptor.ascp)
				.append( ParameterDescriptor.tryAscpDescription )
				.append( '\n' )

				.append( "\n" + ParameterDescriptor.version )
				.append( ParameterDescriptor.versionFlagDescription )
				.append( '\n' )

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
				throw new ParameterException(WebinCliMessage.Cli.INVALID_CONTEXT_ERROR.format(value));
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
		String currentVersion = WebinCli.class.getPackage().getImplementationVersion();
		
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
