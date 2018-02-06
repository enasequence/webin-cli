package uk.ac.ebi.ena.submit;

public enum ContextE {
    transcriptome("Transcriptome assembly: ASSEMBLY_NAME", "TRANSCRIPTOME_ASSEMBLY"),
    assembly("Genome assembly: ASSEMBLY_NAME", "SEQUENCE_ASSEMBLY/");

    private String analysisTitle;
    private String analysisType;

    ContextE(String analysisTitle, String analysisType) {
        this.analysisTitle = analysisTitle;
        this.analysisType = analysisType;
    }

    public String getAnalysisTitle(String assemblyName) {
        return analysisTitle.replace("ASSEMBLY_NAME", assemblyName);
    }

    public String getAnalysisType() {
        return analysisType;
    }
}
