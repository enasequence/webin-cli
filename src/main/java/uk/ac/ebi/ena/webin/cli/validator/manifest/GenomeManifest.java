package uk.ac.ebi.ena.webin.cli.validator.manifest;

public class GenomeManifest extends Manifest<GenomeManifest.FileType> {

    public enum FileType {
        FASTA,
        FLATFILE,
        AGP,
        CHROMOSOME_LIST,
        UNLOCALISED_LIST
    }

    private String assemblyType;
    private String program;
    private String platform;
    private String moleculeType;
    private String coverage;
    private Integer minGapLength;
    private Boolean tpa;

    public String getAssemblyType() {
        return assemblyType;
    }

    public void setAssemblyType(String assemblyType) {
        this.assemblyType = assemblyType;
    }

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

    public String getMoleculeType() {
        return moleculeType;
    }

    public void setMoleculeType(String moleculeType) {
        this.moleculeType = moleculeType;
    }

    public String getCoverage() {
        return coverage;
    }

    public void setCoverage(String coverage) {
        this.coverage = coverage;
    }

    public Integer getMinGapLength() {
        return minGapLength;
    }

    public void setMinGapLength(Integer minGapLength) {
        this.minGapLength = minGapLength;
    }

    public Boolean isTpa() {
        return tpa;
    }

    public void setTpa(Boolean tpa) {
        this.tpa = tpa;
    }
}
