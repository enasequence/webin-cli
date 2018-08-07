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

import uk.ac.ebi.ena.submit.ContextE;

public interface ParameterDescriptor {
    public final static String context    = "-context";
    public final static String outputDir  = "-outputDir";
    public final static String userName   = "-userName";
    public final static String password   = "-password";
    public final static String validate   = "-validate";
    public final static String submit     = "-submit";
    public final static String manifest   = "-manifest";
    public final static String test       = "-test";
    public final static String centerName = "-centerName";
    public final static String version    = "-version";
    public final static String inputDir   = "-inputDir";
    public final static String tryAscp    = "-ascp";
    
    
    //Description
    public final static String contextFlagDescription = "\n\tRequired. Valid values are:"
          + "\n\t\t>> " + "genome"
          + "\n\t\t>> " + "sequence"
          + "\n\t\t>> " + "transcriptome"
          + "\n\t\t>> " + "reads";
    
    public final static String outputDirFlagDescription =
            "\n\tDirectory for output files with the following structure:" +
            "\n\t\t>> <context>/<name>/validate" +
            "\n\t\t>> <context>/<name>/submit" +
            "\n\tThe <name> is the unique name provided in the info file." +
            "\n\tThe 'validate' directory contains validation reports." +
            "\n\tThe 'submit' directory contains the file manifest and the XMLs created during the submission" +
            "\n\tincluding the Receipt XML.";
    
    public final static String userNameFlagDescription = "\n\tRequired. Your submission account name or your e-mail address.";
    public final static String passwordFlagDescription = "\n\tRequired. Your submission account password.";
    public final static String validateFlagDescription = "\n\tValidates the files defined in the manifest file.";
    public final static String submitFlagDescription = "\n\tValidates and submits the files defined in the manifest file.";
    public final static String manifestFlagDescription = "\n\tRequired. Path to a manifest file. The manifest file lists the files within the submission." +
            "\n\tThe manifest file is a text file with two columns separated by a tab:" +
            "\n\t\t>> file type" +
            "\n\t\t>> file path" +
            "\n\tThe following file types are supported:" +
            "\n\t\t>> info" +
            "\n\t\t>> fasta" +
            "\n\t\t>> flatfile" +
            "\n\t\t>> agp (only for genome assemblies)" +
            "\n\t\t>> chromosome_list (only for genome assemblies)" +
            "\n\t\t>> unlocalised_list (only for genome assemblies)" +
            "\n\tMore information is available from: http://ena-docs.readthedocs.io/en/latest/cli.html";
    public final static String testFlagDescription = "\tWhen this option is used it will use the TEST submission system.";
    public static final String centerNameFlagDescription = "\n\tMandatory center name for broker accounts";
    public static final String versionFlagDescription = "\n\tPrints the version number of the program and exists";
    public static final String inputDirFlagDescription = "\n\tInput directory for files declared in manifest file";
    public static final String tryAscpDescription = "\n\tTry to use Aspera Cli instead of FTP file transfer, if available.\n\tNote: Aspera Cli should be installed and path to executable \"ascp\" should be in PATH variable";
}
