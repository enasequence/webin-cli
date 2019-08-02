package uk.ac.ebi.ena.webin.cli.validator.manifest;

public class TranscriptomeManifest extends Manifest<TranscriptomeManifest.FileType> {

    public enum FileType {
        FASTA,
        FLATFILE,
    }

    private String program;
    private String platform;
    private Boolean tpa;

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Boolean isTpa() {
        if (tpa == null) {
            return false;
        }
        return tpa;
    }

    public void setTpa(Boolean tpa) {
        this.tpa = tpa;
    }
}
