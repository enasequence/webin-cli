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

public interface ParameterDescriptor {
    String context    = "-context";
    String outputDir  = "-outputDir";
    String userName   = "-userName";
    String password   = "-password";
    String validate   = "-validate";
    String submit     = "-submit";
    String manifest   = "-manifest";
    String test       = "-test";
    String centerName = "-centerName";
    String version    = "-version";
    String inputDir   = "-inputDir";
    String ascp       = "-ascp";
    String help       = "-help";

    //synonyms
    String userNameSynonym   = "-username";
    String centerNameSynonym = "-centername";
    String outputDirSynonym  = "-outputdir";
    String inputDirSynonym   = "-inputdir";
    
    String OPTION_LINE = "\n\t";
    String MANDATORY_LINE = "[Required] ";
    String VALUE_LINE = "\n\t\t";

    //Description
    String contextFlagDescription =
            OPTION_LINE + MANDATORY_LINE + "The submission type:" +
            VALUE_LINE + "genome" +
            VALUE_LINE + "sequence" +
            VALUE_LINE + "transcriptome" +
            VALUE_LINE + "reads";
    
    String outputDirFlagDescription =
            OPTION_LINE + "Root directory for output files with the following structure:" +
            VALUE_LINE + "<context>/<name>/validate" +
            VALUE_LINE + "<context>/<name>/submit" +
            OPTION_LINE + "The <name> is the unique name provided in the manifest file." +
            OPTION_LINE + "The 'validate' directory contains validation reports." +
            OPTION_LINE + "The 'submit' directory contains the submitted and the receipt XMLs.";

    String userNameFlagDescription = OPTION_LINE + MANDATORY_LINE +"Your submission account name or your e-mail address.";
    String passwordFlagDescription = OPTION_LINE + MANDATORY_LINE +"Your submission account password.";
    String validateFlagDescription = OPTION_LINE + "Only validates the submission.";
    String submitFlagDescription = OPTION_LINE + "First validates and then makes the submission.";

    String manifestFlagDescription =
            OPTION_LINE + MANDATORY_LINE +"Path to the manifest file. The manifest file is" +
            OPTION_LINE + "a text file with two columns separated by a tab: field name and field value." +
            OPTION_LINE + "Full details of the supported fields are available from: " +
            OPTION_LINE + "http://ena-docs.readthedocs.io/en/latest/cli.html";

   String testFlagDescription = "\tUse the test submission system.";
   String centerNameFlagDescription = OPTION_LINE + "Mandatory center name for broker accounts.";
   String versionFlagDescription = OPTION_LINE + "Prints the version number of the program and exits.";
   String inputDirFlagDescription = OPTION_LINE + "Input directory for files declared in the manifest file.";
   String tryAscpDescription =
           OPTION_LINE + "Use Aspera (if Aspera Cli is available) instead of FTP when uploading files." +
           OPTION_LINE + "The path to the installed \"ascp\" program must be in the PATH variable.";
   String helpFlagDescription =
           OPTION_LINE + "Use the -help option to see all command line options.";
}
