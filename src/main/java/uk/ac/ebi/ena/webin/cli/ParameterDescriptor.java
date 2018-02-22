package uk.ac.ebi.ena.webin.cli;

public class ParameterDescriptor {
    public final static String remoteFlag= "-r";
    public final static String helpFlag= "-help";
    public final static String fixFlag ="-fix";
    public final static String organismFlag ="-organism";
    public final static String versionFlag ="-version";
    public final static String context ="-context";
    public final static String outputDir ="-outputDir";
    public final static String userName ="-userName";
    public final static String password ="-password";
    public final static String validate ="-validate";
    public final static String submit ="-submit";
    public final static String upload ="-upload";
    public final static String manifest ="-manifest";
    public final static String test ="-test";

    //Description
    public final static String remoteFlagDescription= "Remote, is this being run outside the EBI";
    public final static String helpFlagDescription= "Displays available options";
    public final static String fixFlagDescription= "Fixes entries in input files";
    public final static String organismFlagDescription= "Scientific Name to validate flatfile";
    public final static String versionFlagDescription ="Displays implementation version of validator Jar";
    public final static String contextFlagDescription ="-context\n" +
            "     Required. Valid values are:\n" +
            "         genome\n" +
            "         transcriptom";
    public final static String outputDirFlagDescription ="  -outputDir\n" +
            "      Directory to write validated files. Validated files are organised into <context>/<name> \n" +
            "      directory structure where <name> is the unique name provided in the info file.";
    public final static String userNameFlagDescription =" -userName\n" +
            "      Required. Your submission account name or your e-mail address.";
    public final static String passwordFlagDescription =" -password\n" +
            "      Required. Your submission account password.";
    public final static String validateFlagDescription ="-validate\n" +
            "      Validates the files defined in the manifest file. Validated files must be uploaded \n" +
            "      using the -upload option and submitted using the -submit option. All three options \n" +
            "      can be given at the same time. Validated and uploaded files are organised into \n" +
            "      <context>/<name> directory  structure where <name> is the unique name provided \n" +
            "      in the info file.";
    public final static String submitFlagDescription ="-submit\n" +
            "      Submits the validated and uploaded files. An accession number is provided, ";
    public final static String uploadFlagDescription ="-upload\n" +
            "      Uploads validated files to Webin upload area. Uploaded files must be \n" +
            "      submitted using the -submit option.";
    public final static String manifestFlagDescription =" -manifest\n" +
            "      Required. Path to a manifest file. The manifest file lists the files within the submission.\n" +
            "      The manifest file is a text file with two columns separated by a tab:\n" +
            "         file type\n" +
            "         file path\n" +
            "      The following file types are supported:\n" +
            "         info\n" +
            "         fasta\n" +
            "         flatfile\n" +
            "         agp (only for genome assemblies)\n" +
            "         chromosome_list (only for genome assemblies)\n" +
            "         unlocalised_list (only for genome assemblies)\n" +
            "      More information is available from: http://ena-docs.readthedocs.io/en/latest/cli.html";
    public final static String testFlagDescription = "When this option is used it will use the TEST submission system.";
}
