package uk.ac.ebi.ena.webin.cli;

import uk.ac.ebi.embl.api.validation.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;

public class WebinCliReporter {

    private final static String DEFAULT_REPORT_NAME = "webin-cli.report";
    private static File defaultReport;

    public static HashSet<Severity> writeSeverity = new HashSet<>(Arrays.asList(
            Severity.INFO,
            Severity.ERROR));


    public static void setDefaultReportDir( File dir ) {
        defaultReport = new File(dir, DEFAULT_REPORT_NAME);
    }

    public static File getDefaultReport() {
        return defaultReport;
    }

    // Create message
    //

    public static ValidationMessage
    createValidationMessage(Severity severity, String error) {
        return createValidationMessage(severity, error, null);
    }

    public static ValidationMessage
    createValidationMessage(Severity severity, String error, Origin origin) {
        ValidationMessage validationMessage = new ValidationMessage<>(severity, ValidationMessage.NO_KEY);
        validationMessage.setMessage(error);
        validationMessage.append(origin);
        return validationMessage;
    }

    // Write to console.
    //

    public static void
    writeToConsole(ValidationResult validationResult) {
        writeMessages(System.out, (s) -> {
            for( ValidationMessage validationMessage: validationResult.getMessages() ) {
                writeMessage(s, validationMessage, null /* targetOrigin */);
            }});
    }

    public static void
    writeToConsole(ValidationMessage validationMessage) {
        writeMessages(System.out, (s) -> writeMessage(s, validationMessage));
    }

    public static void
    writeToConsole(Severity severity, String message) {
        writeMessages(System.out, (s) -> writeMessage(s, createValidationMessage(severity, message, null /* origin */)));
    }

    public static void
    writeToConsole(Severity severity, String message, Origin origin) {
        writeMessages(System.out, (s) -> writeMessage(s, createValidationMessage(severity, message, origin)));
    }

    public static void
    writeToConsole(String message) {
        System.out.println(message.replaceAll("^\\n+", "").replaceAll("\\n+$", ""));
    }

    // Write to file.
    //

    public static void
    writeToFile(File reportFile, ValidationPlanResult validationPlanResult, String targetOrigin) {
        writeMessages(reportFile, (s) -> {
            for( ValidationMessage validationMessage: validationPlanResult.getMessages() ) {
                writeMessage(s, validationMessage, targetOrigin);
            }});
    }

    public static void
    writeToFile(File reportFile, ValidationPlanResult validationPlanResult) {
        writeMessages(reportFile, (s) -> {
            for( ValidationMessage validationMessage: validationPlanResult.getMessages() ) {
                writeMessage(s, validationMessage, null /* targetOrigin */);
            }});
    }

    public static void
    writeToFile(File reportFile, ValidationResult validationResult, String targetOrigin )
    {
        writeMessages(reportFile, (s) -> {
            for( ValidationMessage validationMessage: validationResult.getMessages() ) {
                writeMessage(s, validationMessage, targetOrigin);
            }});
    }

    public static void
    writeToFile(File reportFile, ValidationResult validationResult )
    {
        writeMessages(reportFile, (s) -> {
            for( ValidationMessage validationMessage: validationResult.getMessages() ) {
                writeMessage(s, validationMessage, null /* targetOrigin */);
            }});
    }

    public static void
    writeToFile(File reportFile, ValidationMessage validationMessage )
    {
        writeMessages(reportFile, (s) -> writeMessage(s, validationMessage));
    }

    public static void
    writeToFile(File reportFile, Severity severity, String message, Origin origin )
    {
        writeMessages(reportFile, (s) -> writeMessage(s, createValidationMessage(severity, message, origin)));
    }

    public static void
    writeToFile(File reportFile, Severity severity, String message )
    {
        writeMessages(reportFile, (s) -> writeMessage(s, createValidationMessage(severity, message, null /* origin */)));
    }

    // Write messages
    //

    private interface WriteCallback {
        void write(OutputStream strm) throws IOException;
    }

    private static void
    writeMessages(File reportFile, WriteCallback callback) {
        OutputStream strm = System.out;
        if (reportFile != null) {
            try {
                strm = Files.newOutputStream(reportFile.toPath(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.SYNC);
            } catch (IOException e) {
                //
            }
        }
        writeMessages(strm, callback);
    }

    private static void
    writeMessages(OutputStream strm, WriteCallback callback) {
        try {
            callback.write(strm);
        }
        catch( IOException e ) {
            //
        }
        finally {
            if (strm != System.out &&
                strm != System.err) {
                try {
                    strm.close();
                } catch (IOException e) {
                    //
                }
            }
        }
    }

    // Write message
    //

    private static void
    writeMessage(OutputStream strm, ValidationMessage validationMessage ) throws IOException {
        writeMessage(strm, validationMessage, null);
    }

    private static void
    writeMessage(OutputStream strm, ValidationMessage validationMessage, String targetOrigin ) throws IOException {
        if (writeSeverity.contains(validationMessage.getSeverity())) {
            strm.write(formatMessage(validationMessage, targetOrigin).getBytes(StandardCharsets.UTF_8));
        }
    }

    // Format message
    //

    private static String
    formatMessage(ValidationMessage validationMessage, String targetOrigin)
    {
        try
        {
            StringWriter str = new StringWriter();
            validationMessage.writeMessage( str, targetOrigin ); // ValidationMessage::messageFormatter
            return str.toString();
        } catch ( IOException e )
        {
            return e.toString();
        }
    }
}
