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

    //Description
    public final static String remoteFlagDescription= "Remote, is this being run outside the EBI";
    public final static String helpFlagDescription= "Displays available options";
    public final static String fixFlagDescription= "Fixes entries in input files";
    public final static String organismFlagDescription= "Scientific Name to validate flatfile";
    public final static String versionFlagDescription ="Displays implementation version of validator Jar";
    public final static String contextFlagDescription ="valid values: assembly";
    public final static String outputDirFlagDescription ="Directory to write output files";
    public final static String userNameFlagDescription ="Your account Username";
    public final static String passwordFlagDescription ="Your account password";
    public final static String validateFlagDescription ="Optional, validates on data files.";
    public final static String submitFlagDescription ="Optional, Submits validated files.";
    public final static String uploadFlagDescription ="Optional, Uploads validated files.";
    public final static String manifestFlagDescription ="Required, path the manifest files.";
}
