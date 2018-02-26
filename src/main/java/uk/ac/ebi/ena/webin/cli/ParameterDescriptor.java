package uk.ac.ebi.ena.webin.cli;

public class ParameterDescriptor {
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
    public final static String contextFlagDescription = "\n\tRequired. Valid values are:" +
            "\n\t\t>> genome" +
            "\n\t\t>> transcriptome";
    public final static String outputDirFlagDescription = "\n\tDirectory to write validated files. Validated files are organised into <context>/<name>\n\t" +
            "directory structure where <name> is the unique name provided in the info file,";
    public final static String userNameFlagDescription = "\n\tRequired. Your submission account name or your e-mail address,";
    public final static String passwordFlagDescription = "\n\tRequired. Your submission account password,";
    public final static String validateFlagDescription = "\n\tValidates the files defined in the manifest file. Validated files must be uploaded" +
            "\n\tusing the -upload option and submitted using the -submit option. All three options" +
            "\n\ttcan be given at the same time. Validated and uploaded files are organised into" +
            "\t\n<context>/<name> directory  structure where <name> is the unique name provided" +
            "\n\tin the info file,";
    public final static String submitFlagDescription = "\n\tSubmits the validated and uploaded files. An accession number is provided, ";
    public final static String uploadFlagDescription = "\n\tUploads validated files to Webin upload area. Uploaded files must be submitted using the -submit option,";
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
    public final static String testFlagDescription = "\tWhen this option is used it will use the TEST submission system,";
}
