/*
 * Copyright 2018-2021 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.sift.MDCBasedDiscriminator;
import ch.qos.logback.classic.sift.SiftingAppender;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.util.Duration;
import de.vandermeer.asciitable.AT_Renderer;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_FixedWidth;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import org.apache.commons.lang3.StringUtils;
import org.fusesource.jansi.AnsiConsole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import picocli.CommandLine;
import uk.ac.ebi.ena.webin.cli.entity.Version;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldDefinition;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileCount;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileGroup;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderBuilder;
import uk.ac.ebi.ena.webin.cli.manifest.processor.CVFieldProcessor;
import uk.ac.ebi.ena.webin.cli.service.LoginService;
import uk.ac.ebi.ena.webin.cli.service.SubmitService;
import uk.ac.ebi.ena.webin.cli.service.VersionService;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.upload.ASCPService;
import uk.ac.ebi.ena.webin.cli.upload.FtpService;
import uk.ac.ebi.ena.webin.cli.upload.UploadService;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class WebinCli {
    public final static int SUCCESS = 0;
    public final static int SYSTEM_ERROR = 1;
    public final static int USER_ERROR = 2;
    public final static int VALIDATION_ERROR = 3;

    private final static String LOG_FILE_NAME = "webin-cli.report";
    private final static Logger log = LoggerFactory.getLogger(WebinCli.class);

    private final static String SIFTING_APPENDER_NAME = "DEFAULT_SIFTING_APPENDER";
    private final static String SIFTING_APPENDER_DISCRIMINATOR_KEY = "uniqueKey";
    private final static String SIFTING_APPENDER_DISCRIMINATOR_DEFAULT_VALUE = "unknown";
    private final static AtomicBoolean SIFTING_APPENDER_CREATED = new AtomicBoolean(false);
    private final static String MDC_LOG_FILE_KEY = "logFile";

    private final WebinCliParameters parameters;
    private final WebinCliExecutor<?, ?> executor;

    private final String fileAppenderName = "FILE_APPENDER_" + UUID.randomUUID().toString();

    public static void
    main(String... args) {
        System.exit(__main(args));
    }

    private static int
    __main(String... args) {
        System.setProperty("picocli.trace", "OFF");
        try {
            WebinCliCommand cmd = parseCmd(args);
            if (null == cmd) {
                return USER_ERROR;
            }

            if (cmd.help || cmd.fields || cmd.version) {
                return SUCCESS;
            }

            checkVersion(cmd.test);

            WebinCli webinCli = new WebinCli(cmd);
            webinCli.execute();

            return SUCCESS;
        } catch (WebinCliException ex) {
            log.error(ex.getMessage(), ex);
            switch (ex.getErrorType()) {
                case USER_ERROR:
                    return USER_ERROR;
                case VALIDATION_ERROR:
                    return VALIDATION_ERROR;
                default:
                    return SYSTEM_ERROR;
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return SYSTEM_ERROR;
        }
    }

    public WebinCli(WebinCliCommand cmd) {
        this(initParameters(getSubmissionAccount(cmd), getAuthToken(cmd), cmd));
    }

    public WebinCli(WebinCliParameters parameters) {
        this.parameters = parameters;
        this.executor = parameters.getContext().createExecutor(parameters);

        // initTimedConsoleLogger();
        initFileLogging();
    }

    public static WebinCliParameters initParameters(
        String submissionAccount, String authToken, WebinCliCommand cmd) {
        if (!cmd.inputDir.isDirectory())
            throw WebinCliException.userError(WebinCliMessage.CLI_INPUT_PATH_NOT_DIR.format(cmd.inputDir.getPath()));
        if (!cmd.outputDir.isDirectory())
            throw WebinCliException.userError(WebinCliMessage.CLI_OUTPUT_PATH_NOT_DIR.format(cmd.outputDir.getPath()));
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setSubmissionAccount(submissionAccount);
        parameters.setWebinAuthToken(authToken);
        parameters.setContext(cmd.context);
        parameters.setManifestFile(cmd.manifest);
        parameters.setInputDir(cmd.inputDir);
        parameters.setOutputDir(cmd.outputDir);
        parameters.setUsername(cmd.userName);
        parameters.setPassword(cmd.password);
        parameters.setCenterName(cmd.centerName);
        parameters.setValidate(cmd.validate);
        parameters.setQuick(cmd.quick);
        parameters.setSubmit(cmd.submit);
        parameters.setTest(cmd.test);
        parameters.setAscp(cmd.ascp);
        return parameters;
    }

    private void initFileLogging() {
        if (!SIFTING_APPENDER_CREATED.get()) {
            synchronized (WebinCli.class) {
                if (!SIFTING_APPENDER_CREATED.get()) {
                    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

                    MDCBasedDiscriminator discriminator = new MDCBasedDiscriminator();
                    discriminator.setContext(loggerContext);
                    discriminator.setKey(SIFTING_APPENDER_DISCRIMINATOR_KEY);
                    discriminator.setDefaultValue(SIFTING_APPENDER_DISCRIMINATOR_DEFAULT_VALUE);
                    discriminator.start();

                    SiftingAppender siftingAppender = new SiftingAppender();
                    siftingAppender.setAppenderFactory(
                        (context,discriminatorValue) -> createFileAppender(context, discriminatorValue));
                    siftingAppender.setDiscriminator(discriminator);
                    siftingAppender.setTimeout(Duration.buildBySeconds(300));
                    siftingAppender.setContext(loggerContext);
                    siftingAppender.setName(SIFTING_APPENDER_NAME);
                    siftingAppender.start();

                    ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
                        Logger.ROOT_LOGGER_NAME);
                    logger.addAppender(siftingAppender);

                    SIFTING_APPENDER_CREATED.set(true);
                }
            }
        }

        String logFile = new File(createOutputDir(parameters.getOutputDir(), "."), LOG_FILE_NAME).getAbsolutePath();

        MDC.put(SIFTING_APPENDER_DISCRIMINATOR_KEY, fileAppenderName);
        MDC.put(MDC_LOG_FILE_KEY, logFile);
    }

    private static Appender createFileAppender(Context context, String discriminator) {
        String filePath;

        if (discriminator.equals(SIFTING_APPENDER_DISCRIMINATOR_DEFAULT_VALUE)
            || (filePath = MDC.get(MDC_LOG_FILE_KEY)) == null) {
            return null;
        }

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%d{\"yyyy-MM-dd'T'HH:mm:ss\"} %-5level: %msg%n");
        encoder.start();

        FileAppender fileAppender = new FileAppender<>();
        fileAppender.setContext(context);
        fileAppender.setEncoder(encoder);
        fileAppender.setFile(filePath);
        fileAppender.setAppend(false);
        fileAppender.setName(discriminator);
        fileAppender.start();

        log.info("Creating report file: " + filePath);

        return fileAppender;
    }

    private void cleanupFileAppender() {
        //MDC should be cleared before the appender is looked up below to avoid getting the file appender created
        //in case it was not created earlier.
        MDC.remove(SIFTING_APPENDER_DISCRIMINATOR_KEY);
        MDC.remove(MDC_LOG_FILE_KEY);

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
            Logger.ROOT_LOGGER_NAME);

        /**
         * Although, sifting appender automatically removes the appender eventually after the timeout,
         * this needs to be done now so that the files created by this instance can be deleted right
         * after the executing finishes.
         */
        SiftingAppender siftingAppender = (SiftingAppender) logger.getAppender(SIFTING_APPENDER_NAME);

        Appender appender = siftingAppender.getAppenderTracker()
            .getOrCreate(fileAppenderName, System.currentTimeMillis());

        if (appender != null && appender instanceof FileAppender) {
            appender.stop();
        }
    }

    public void
    execute() {
        try {
            executor.readManifest();

            if (parameters.isValidate() || executor.readSubmissionBundle() == null) {
                validate(executor);
            }

            if (parameters.isSubmit()) {
                submit(executor);
            }
        } catch (WebinCliException ex) {
            throw ex;
        } catch (Exception ex) {
            throw WebinCliException.systemError(ex);
        } finally {
            cleanupFileAppender();
        }
    }

    private void
    validate(WebinCliExecutor<?, ?> executor) {
        try {
            executor.validateSubmission();

            executor.prepareSubmissionBundle();

            log.info(WebinCliMessage.CLI_VALIDATE_SUCCESS.text());

        } catch (WebinCliException ex) {
            switch (ex.getErrorType()) {
                case USER_ERROR:
                    throw WebinCliException.userError(
                        ex, getUserError(ex.getMessage(), executor.getValidationDir().toString()));

                case VALIDATION_ERROR:
                    throw WebinCliException.validationError(
                        ex, getUserError(ex.getMessage(), executor.getValidationDir().toString()));

                case SYSTEM_ERROR:
                    throw WebinCliException.systemError(
                        ex, getSystemError(ex.getMessage(), executor.getValidationDir().toString()));
            }
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            throw WebinCliException.systemError(ex, getSystemError(
                null == ex.getMessage() ? sw.toString() : ex.getMessage(), executor.getValidationDir().toString()));
        }
    }

    private void
    submit(WebinCliExecutor<?, ?> executor) {
        SubmissionBundle bundle = executor.readSubmissionBundle();

        UploadService fileUploadService = parameters.isAscp() && new ASCPService().isAvailable() ? new ASCPService() : new FtpService();

        try {
            fileUploadService.connect(parameters.getFileUploadServiceUserName(), parameters.getPassword());
            fileUploadService.upload(bundle.getUploadFileList(), bundle.getUploadDir(), executor.getParameters().getInputDir().toPath());
            log.info(WebinCliMessage.CLI_UPLOAD_SUCCESS.text());

        } catch (WebinCliException e) {
            throw WebinCliException.error(e, WebinCliMessage.CLI_UPLOAD_ERROR.format(e.getErrorType().text));
        } finally {
            fileUploadService.disconnect();
        }

        try {
            SubmitService submitService = new SubmitService.Builder()
                .setSubmitDir(bundle.getSubmitDir().getPath())
                .setUserName(parameters.getWebinServiceUserName())
                .setPassword(parameters.getPassword())
                .setTest(parameters.isTest())
                .build();

            submitService.doSubmission(bundle.getXMLFileList(), bundle.getCenterName(),
                getVersionForSubmission(parameters.getWebinSubmissionTool()),
                bundle.getManifestMd5(), getManifestFileContent());

        } catch (WebinCliException e) {
            throw WebinCliException.error(e, WebinCliMessage.CLI_SUBMIT_ERROR.format(e.getErrorType().text));
        }
    }

    String getManifestFileContent() {
        try {
            return new String(Files.readAllBytes(getParameters().getManifestFile().toPath()));
        } catch (IOException ioe) {
            throw WebinCliException.userError( "Exception thrown while reading manifest file", ioe.getMessage());
        }
    }

    private static WebinCliCommand parseCmd(String... args) {
        AnsiConsole.systemInstall();
        WebinCliCommand params = new WebinCliCommand();
        CommandLine commandLine = new CommandLine(params);
        commandLine.setExpandAtFiles(false);

        String cmd = "java -jar webin-cli-" + getVersionForUsage() + ".jar";
        commandLine.setCommandName(cmd);

        try {
            commandLine.parse(args);
            if (commandLine.isUsageHelpRequested()) {
                if (params.help) {
                    commandLine.usage(System.out);
                }
                if (params.fields) {
                    if (params.context != null) {
                        printManifestHelp(params.context, System.out);
                    } else {
                        for (WebinCliContext context : WebinCliContext.values()) {
                            printManifestHelp(context, System.out);
                        }
                    }
                }
                return params;
            }

            if (commandLine.isVersionHelpRequested()) {
                commandLine.printVersionHelp(System.out);
                return params;
            }

            if (params.password != null) {
                // Password from command line
            } else if (params.passwordEnv != null) {
                params.password = System.getenv(params.passwordEnv);
                if (params.password == null) {
                    log.error("Could not read password from the environment variable: " + params.passwordEnv);
                    printHelp();
                    return null;
                }
            } else if (params.passwordFile != null) {
                try {
                    params.password = new String(Files.readAllBytes(params.passwordFile.toPath()));
                    if (params.password.trim().isEmpty()) {
                        log.error("Could not read password from the file: " + params.passwordFile.getPath());
                        printHelp();
                        return null;
                    }
                } catch (IOException ex) {
                    log.error("Could not read password from the file: " + params.passwordFile.getPath());
                    printHelp();
                    return null;
                }
            } else {
                log.error("Password must be provided using one of the following options: " +
                    String.join(", "
                        , WebinCliCommand.Options.password
                        , WebinCliCommand.Options.passwordEnv
                        , WebinCliCommand.Options.passwordFile));
                printHelp();
                return null;
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

            if (!params.validate && !params.submit) {
                log.error("Either -validate or -submit option must be provided.");
                printHelp();
                return null;
            }

            return params;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            printHelp();
            return null;
        }
    }

    public static void printManifestHelp(WebinCliContext context, PrintStream out) {
        ManifestReader<?> manifestReader =
            new ManifestReaderBuilder(context.getManifestReaderClass()).build();
        out.println();
        out.println("Manifest fields for '" + context.name() + "' context:");
        out.println();
        printManifestFieldHelp(manifestReader, out);
        out.println();
        out.println("Data files for '" + context.name() + "' context:");
        out.println();
        printManifestFileGroupHelp(manifestReader, out);
    }

    private static void printManifestFieldHelp(ManifestReader<?> manifestReader, PrintStream out) {
        AsciiTable table = new AsciiTable();
        AT_Renderer renderer = AT_Renderer.create();
        CWC_FixedWidth cwc = new CWC_FixedWidth();
        cwc.add(20);
        cwc.add(11);
        cwc.add(45);
        renderer.setCWC(cwc);
        table.setRenderer(renderer);
        table.addRule();
        table.addRow("Field", "Cardinality", "Description");

        Comparator<ManifestFieldDefinition> comparator = (f1, f2) ->
        {
            ManifestFieldType t1 = f1.getType();
            ManifestFieldType t2 = f2.getType();
            int min1 = f1.getRecommendedMinCount();
            int min2 = f2.getRecommendedMinCount();
            if (t1 == t2 && min1 == min2) {
                return 0;
            }
            if (t1 == t2 && min1 > min2) {
                return -1;
            }
            if (t1 == t2 && min1 < min2) {
                return 1;
            }
            if (t1 == ManifestFieldType.META) {
                return -1;
            }
            return 1;
        };
        manifestReader.getFields().stream()
            .filter(field -> field.getRecommendedMaxCount() > 0)
            .sorted(comparator)
            .forEach(field ->
                printManifestFieldHelp(table, field)
            );
        table.addRule();
        table.setPadding(0);
        table.setTextAlignment(TextAlignment.LEFT);
        out.println(table.render());
    }

    private static void printManifestFieldHelp(AsciiTable table, ManifestFieldDefinition field) {
        String name = field.getName();
        if (field.getSynonym() != null) {
            name += " (" + field.getSynonym() + ")";
        }

        String cardinality;
        int minCount = field.getRecommendedMinCount();
        int maxCount = field.getRecommendedMaxCount();
        if (field.getType() == ManifestFieldType.META) {
            cardinality = minCount > 0 ? "Mandatory" : "Optional";
        } else {
            if (minCount == maxCount) {
                cardinality = minCount + " file";
            } else {
                cardinality = minCount + "-" + maxCount + " files";
            }
        }
        String value = "";
        if (field.getType() == ManifestFieldType.META) {
            for (ManifestFieldProcessor processor : field.getFieldProcessors()) {
                if (processor instanceof CVFieldProcessor) {
                    value = ": <br/>* " + ((CVFieldProcessor) processor).getValues().stream()
                        .collect(Collectors.joining("<br/>* "));
                }
            }
        }

        StringBuilder attHelpText = new StringBuilder();
        if (!field.getFieldAttributes().isEmpty()) {
            field.getFieldAttributes().stream().forEach(att -> {
                attHelpText.append("<br/>" + att.getName() + " attribute");

                for (ManifestFieldProcessor processor : att.getFieldProcessors()) {
                    if (processor instanceof CVFieldProcessor) {
                        attHelpText.append(":<br/>  * " + ((CVFieldProcessor) processor).getValues().stream()
                            .collect(Collectors.joining("<br/>  * ")));
                    }
                }
            });
        }

        table.addRule();
        table.addRow(name, cardinality, field.getDescription() + value + attHelpText);
    }

    private static void printManifestFileGroupHelp(ManifestReader<?> manifestReader, PrintStream out) {
        List<ManifestFieldDefinition> fields = manifestReader.getFields()
            .stream()
            .filter(field -> field.getType() == ManifestFieldType.FILE)
            .collect(Collectors.toList());

        List<ManifestFileGroup> groups = manifestReader.getFileGroups()
            .stream()
            .sorted(Comparator.comparingInt(ManifestFileGroup::getFileCountsSize))
            .collect(Collectors.toList());

        AsciiTable table = new AsciiTable();
        AT_Renderer renderer = AT_Renderer.create();
        CWC_FixedWidth cwc = new CWC_FixedWidth();
        int tableWidth = 80;
        int descriptionWidth = 30;
        cwc.add(descriptionWidth);
        fields.forEach(field ->
            cwc.add((tableWidth - descriptionWidth - 2 - fields.size()) / fields.size()));
        renderer.setCWC(cwc);
        table.setRenderer(renderer);
        table.addRule();

        ArrayList<String> row = new ArrayList<>();
        row.add("Data files");
        fields.stream().forEach(field -> row.add(field.getName()));
        table.addRow(row);
        table.addRule();

        groups.stream().forEach(group -> {
            row.clear();
            row.add(group.getDescription());
            fields.stream().forEach(field ->
                row.add(printManifestFileCountHelp(field, group)));
            table.addRow(row);
            table.addRule();
        });
        table.setPadding(0);
        table.setTextAlignment(TextAlignment.LEFT);
        out.println(table.render());
    }

    private static String printManifestFileCountHelp(ManifestFieldDefinition field, ManifestFileGroup group) {
        ManifestFileCount count = null;
        for (ManifestFileCount fileCount : group.getFileCounts()) {
            if (field.getName().equals(fileCount.getFileType())) {
                count = fileCount;
                break;
            }
        }
        if (count == null) {
            return "";
        }
        if (count.getMaxCount() != null) {
            if (count.getMinCount() == count.getMaxCount()) {
                return String.valueOf(count.getMinCount());
            }
            return count.getMinCount() + "-" + count.getMaxCount();
        }
        return ">=" + count.getMinCount();
    }

    public WebinCliParameters getParameters() {
        return parameters;
    }

    public WebinCliExecutor<?, ?> getExecutor() {
        return executor;
    }

    private static void
    printHelp() {
        log.info("Please use " + WebinCliCommand.Options.help + " option to see all command line options.");
    }

    public static String
    getVersionForSubmission(WebinSubmissionTool webinSubmissionTool) {
        String version = getVersion();

        switch (webinSubmissionTool) {
            case WEBIN_CLI_REST: {
                return webinSubmissionTool.getToolName();
            }

            default: {
                return String.format("%s:%s", WebinCli.class.getSimpleName(), null == version ? "" : version);
            }
        }
    }

    public static String
    getVersionForUsage() {
        String version = getVersion();
        return String.format("%s", null == version ? "?" : version);
    }

    private static String
    getVersion() {
        return WebinCli.class.getPackage().getImplementationVersion();
    }

    public static String getSubmissionAccount(WebinCliCommand cmd) {
        // Return the Webin-N submission account returned by the login service.
        // This may be different from the username used to login as email address
        // or su-Webin- superuser can also be used as a username.
        return new LoginService(cmd.userName, cmd.password, cmd.test).login();
    }

    public static String getAuthToken(WebinCliCommand cmd) {
        // Return the Webin authentication token for the given user.
        return new LoginService(cmd.userName, cmd.password, cmd.test).getAuthToken();
    }

    private static void checkVersion(boolean test) {
        String currentVersion = getVersion();

        if (null == currentVersion || currentVersion.isEmpty())
            return;

        Version version = new VersionService.Builder()
            .setTest(test)
            .build().getVersion(currentVersion);

        log.info(WebinCliMessage.CLI_CURRENT_VERSION.format(currentVersion));

        if (!version.valid) {
            throw WebinCliException.userError(WebinCliMessage.CLI_UNSUPPORTED_VERSION.format(
                version.minVersion,
                version.latestVersion));
        }

        if (version.expire) {
            log.info(WebinCliMessage.CLI_EXPIRYING_VERSION.format(
                new SimpleDateFormat("dd MMM yyyy").format(version.nextMinVersionDate),
                version.nextMinVersion,
                version.latestVersion));
        } else if (version.update) {
            log.info(WebinCliMessage.CLI_NEW_VERSION.format(version.latestVersion));
        }

        if (version.comment != null) {
            log.info(version.comment);
        }
    }

    public static File
    createOutputDir(File outputDir, String... dirs) throws WebinCliException {
        if (outputDir == null) {
            throw WebinCliException.systemError(WebinCliMessage.CLI_MISSING_OUTPUT_DIR_ERROR.text());
        }

        String[] safeDirs = getSafeOutputDirs(dirs);

        Path p;

        try {
            p = Paths.get(outputDir.getPath(), safeDirs);
        } catch (InvalidPathException ex) {
            throw WebinCliException.systemError(WebinCliMessage.CLI_CREATE_DIR_ERROR.format(ex.getInput()));
        }

        File dir = p.toFile();

        if (!dir.exists() && !dir.mkdirs()) {
            throw WebinCliException.systemError(WebinCliMessage.CLI_CREATE_DIR_ERROR.format(dir.getPath()));
        }

        return dir;
    }

    public static String
    getSafeOutputDir(String dir) {
        return dir
            .replaceAll("[^a-zA-Z0-9-_\\.]", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_+(?=[^_])", "")
            .replaceAll("(?<=[^_])_+$", "");
    }

    public static String[]
    getSafeOutputDirs(String... dirs) {
        return Arrays.stream(dirs)
            .map(dir -> getSafeOutputDir(dir))
            .toArray(String[]::new);
    }

    private String getUserOrSystemError(String message, String validationDir, String errorType) {
        String str = WebinCliMessage.CLI_VALIDATE_USER_OR_SYSTEM_ERROR.format(errorType);

        if (!StringUtils.isBlank(message)) {
            str += " : " + message;
        }

        str += ".";

        if ( getParameters().getWebinSubmissionTool() == WebinSubmissionTool.WEBIN_CLI) {
            str += " Please check validation reports for further information: " + validationDir;
        }

        return str;
    }

    private String getUserError(String message, String validationDir) {
        return getUserOrSystemError(message, validationDir, "user");
    }

    private String getSystemError(String message, String validationDir) {
        return getUserOrSystemError(message, validationDir, "system");
    }
}
