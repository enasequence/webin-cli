package uk.ac.ebi.ena.webin.cli;

import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.List;

public class
WebinCliOptions
{
    @Parameter()
    public List<String> unrecognisedOptions = new ArrayList<>();

    @Parameter(names = Option.context, description = Description.context, required = true,validateWith = WebinCli.ContextValidator.class, order = 0)
    public String context;

    @Parameter(names = Option.manifest, description = Description.manifest, required = true,validateWith = WebinCli.ManifestFileValidator.class, order = 1)
    public String manifest;

    @Parameter(names = { Option.userName, Option.userNameSynonym }, description = Description.userName, required = true, order = 2)
    public String userName;

    @Parameter(names = Option.password, description = Description.password, required = true, order = 3)
    public String password;

    @Parameter(names = { Option.inputDir, Option.inputDirSynonym }, description = Description.inputDir, order = 4)
    public String inputDir = ".";

    @Parameter(names = { Option.outputDir, Option.outputDirSynonym }, description = Description.outputDir,validateWith = WebinCli.OutputDirValidator.class, order = 5)
    public String outputDir;

    @Parameter(names = { Option.centerName, Option.centerNameSynonym }, description = Description.centerName, order = 6)
    public String centerName;

    @Parameter(names = Option.validate, description = Description.validate, order = 7)
    public boolean validate;

    @Parameter(names = Option.submit, description = Description.submit, order = 8)
    public boolean submit;

    @Parameter(names = Option.test, description = Description.test, order = 9)
    public boolean test;

    @Parameter(names = Option.ascp, description = Description.ascp, order = 10)
    public boolean ascp;

    @Parameter(names = Option.help, description = Description.help, help = true, order = 11)
    public boolean help;

    @Parameter(names = Option.version, description = Description.version, order = 12)
    public boolean version;

    public interface Option {
        String context    = "-context";
        String manifest   = "-manifest";
        String userName   = "-userName";
        String password   = "-password";
        String inputDir   = "-inputDir";
        String outputDir  = "-outputDir";
        String centerName = "-centerName";
        String submit     = "-submit";
        String validate   = "-validate";
        String test       = "-test";
        String ascp       = "-ascp";
        String version    = "-version";
        String help       = "-help";
        String userNameSynonym   = "-username";
        String centerNameSynonym = "-centername";
        String outputDirSynonym  = "-outputdir";
        String inputDirSynonym   = "-inputdir";
    }

    public interface Description {
        String context = "The submission type: reads, genome, transcriptome or sequence.";
        String manifest =
                "Path to the manifest file. It is a tab separated text file " +
                "with two columns: field name and field value. " +
                "Full details of the supported fields are available from: " +
                "http://ena-docs.readthedocs.io/en/latest/cli.html";
        String userName = "Your submission account name or your e-mail address.";
        String password = "Your submission account password.";
        String inputDir = "Input directory for the files declared in the manifest file.";
        String outputDir =
                "Root directory for the output files written into the following " +
                "directory structure: <context>/<name>/<validate,process,submit>. " +
                "The <context> is given using the -context option. " +
                "The <name> is a unique name from the manifest file. " +
                "The <validate> directory contains validation reports. " +
                "The <process> directory contains temporary files. " +
                "The <submit> directory contains submitted files and receipt XMLs.";
        String centerName = "Mandatory center name for broker accounts.";
        String submit = "Validate, upload and submit the files.";
        String validate = "Validate the files without uploading or submitting them.";
        String test = "Use the test submission service.";
        String ascp =
                "Use Aspera (if Aspera Cli is available) instead of FTP when uploading files. " +
                "The path to the installed \"ascp\" program must be in the PATH variable.";
        String version = "Show the version number of the program.";
        String help = "Show all command line options.";
    }
}
