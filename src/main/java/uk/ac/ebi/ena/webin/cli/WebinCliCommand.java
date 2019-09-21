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

import picocli.CommandLine;
import picocli.CommandLine.Option;

@CommandLine.Command(
        headerHeading = "%n",
        descriptionHeading = "%nDescription:%n%n",
        optionListHeading = "%nOptions:%n",
        description = "Validate and submit files to ENA using the Webin submission service. Detailed instructions are available from:\nhttps://ena-docs.readthedocs.io/en/latest/cli.html",
        versionProvider = WebinCliCommand.VersionProvider.class,
        footer=" Exit codes: 0=SUCCESS, 1=INTERNAL ERROR, 2=USER ERROR, 3=VALIDATION ERROR",
        sortOptions = false)
public class
WebinCliCommand
{
    public static class VersionProvider implements CommandLine.IVersionProvider {
        public String[] getVersion() {
            return new String[] {WebinCli.getVersionForUsage() };
        }
    }

    @Option(names = Options.context, description = Descriptions.context, paramLabel= "TYPE", required = true, order = 0)
    public WebinCliContext context;

    @Option(names = Options.manifest, description = Descriptions.manifest, paramLabel= "FILE", required = true, order = 1)
    public File manifest;

    @Option(names = { Options.userName, Options.userNameSynonym }, paramLabel= "USER", description = Descriptions.userName, required = true, order = 2)
    public String userName;

    @Option(names = Options.password, description = Descriptions.password, paramLabel= "PASSWORD", required = true, order = 3)
    public String password;

    @Option(names = { Options.inputDir, Options.inputDirSynonym }, description = Descriptions.inputDir, paramLabel= "DIRECTORY", order = 4)
    public File inputDir;

    @Option(names = { Options.outputDir, Options.outputDirSynonym }, description = Descriptions.outputDir, paramLabel= "DIRECTORY", order = 5)
    public File outputDir;

    @Option(names = { Options.centerName, Options.centerNameSynonym }, description = Descriptions.centerName, paramLabel= "CENTER", order = 6)
    public String centerName;

    @Option(names = Options.validate, description = Descriptions.validate, order = 7)
    public boolean validate;

    @Option(names = Options.submit, description = Descriptions.submit, order = 8)
    public boolean submit;

    @Option(names = Options.test, description = Descriptions.test, order = 9)
    public boolean test;

    @Option(names = Options.ascp, description = Descriptions.ascp, order = 10)
    public boolean ascp;

    @Option(names = Options.help, description = Descriptions.help, usageHelp = true, order = 100)
    public boolean help;

    @Option(names = Options.fields, description = Descriptions.fields, usageHelp = true, order = 101)
    public boolean fields;

    @Option(names = Options.version, description = Descriptions.version, versionHelp = true, order = 101)
    public boolean version;

    public interface Options {
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
        String help       = "-help";
        String fields     = "-fields";
        String version    = "-version";
        String userNameSynonym   = "-username";
        String centerNameSynonym = "-centername";
        String outputDirSynonym  = "-outputdir";
        String inputDirSynonym   = "-inputdir";
    }

    public interface Descriptions {
        String context = "Submission type: ${COMPLETION-CANDIDATES}";
        String manifest =
                "Manifest text file containing file and metadata fields.";
        String userName = "Webin submission account name or e-mail address.";
        String password = "Webin submission account password.";
        String inputDir =
                "Root directory for the files declared in the manifest file. " +
                "By default the current working directory is used as the input directory.";
        String outputDir =
                "Root directory for any output files written in " +
                "<context>/<name>/<validate,process,submit> directory structure. " +
                "By default the manifest file directory is used as the output directory. " +
                "The <name> is the unique name from the manifest file. The " +
                "validation reports are written in the <validate> sub-directory. ";
        String centerName = "Mandatory center name for broker accounts.";
        String submit = "Validate, upload and submit files.";
        String validate = "Validate files without uploading or submitting them.";
        String test = "Use the test submission service.";
        String ascp =
                "Use Aspera (if Aspera Cli is available) instead of FTP when uploading files. " +
                "The path to the installed \"ascp\" program must be in the PATH variable.";
        String help =
                "Show this help message and exit.";
        String fields =
                "Show manifest fields for all contexts or for the given -context.";
        String version =
                "Print version information and exit.";
    }
}
