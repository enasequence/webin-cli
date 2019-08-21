package uk.ac.ebi.ena.webin.cli.validator.manifest;

public class ReadsManifest extends Manifest<ReadsManifest.FileType> {

  public enum FileType {
    BAM,
    CRAM,
    FASTQ
  }

  private String platform;
  private String instrument;
  private Integer insertSize;
  private String libraryConstructionProtocol;
  private String libraryName;
  private String librarySource;
  private String librarySelection;
  private String libraryStrategy;
  private boolean paired = false;
  private String qualityScore;
  private Integer pairingHorizon = 500_000_000;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public Integer getInsertSize() {
        return insertSize;
    }

    public void setInsertSize(Integer insertSize) {
        this.insertSize = insertSize;
    }

    public String getLibraryConstructionProtocol() {
        return libraryConstructionProtocol;
    }

    public void setLibraryConstructionProtocol(String libraryConstructionProtocol) {
        this.libraryConstructionProtocol = libraryConstructionProtocol;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public void setLibraryName(String libraryName) {
        this.libraryName = libraryName;
    }

    public String getLibrarySource() {
        return librarySource;
    }

    public void setLibrarySource(String librarySource) {
        this.librarySource = librarySource;
    }

    public String getLibrarySelection() {
        return librarySelection;
    }

    public void setLibrarySelection(String librarySelection) {
        this.librarySelection = librarySelection;
    }

    public String getLibraryStrategy() {
        return libraryStrategy;
    }

    public void setLibraryStrategy(String libraryStrategy) {
        this.libraryStrategy = libraryStrategy;
    }

    public boolean isPaired() {
        return paired;
    }

    public void setPaired(boolean paired) {
        this.paired = paired;
    }

    public String getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(String qualityScore) {
        this.qualityScore = qualityScore;
    }

    public Integer getPairingHorizon() {
        return pairingHorizon;
    }

    public void setPairingHorizon(Integer pairingHorizon) {
        this.pairingHorizon = pairingHorizon;
    }
}

