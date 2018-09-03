package uk.ac.ebi.ena.assembly;

import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.ena.manifest.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class GenomeAssemblyManifest extends ManifestReader
{
    public interface
    Fields {
        String NAME = "NAME";
        String ASSEMBLYNAME = "ASSEMBLYNAME";
        String STUDY = "STUDY";
        String SAMPLE = "SAMPLE";
        String COVERAGE = "COVERAGE";
        String PROGRAM = "PROGRAM";
        String PLATFORM = "PLATFORM";
        String MINGAPLENGTH = "MINGAPLENGTH";
        String MOLECULETYPE = "MOLECULETYPE";
        String ASSEMBLY_TYPE = "ASSEMBLY_TYPE";
        String TPA = "TPA";
        String CHROMOSOME_LIST = "CHROMOSOME_LIST";
        String UNLOCALISED_LIST = "UNLOCALISED_LIST";
        String FASTA = "FASTA";
        String FLATFILE = "FLATFILE";
        String AGP = "AGP";
    }

    private String name;
    private String studyId;
    private String sampleId;
    private String coverage;
    private String program;
    private String platform;
    private Integer minGapLength;
    private String moleculeType;
    private String assemblyType;
    private Boolean tpa;

    private File chromosomeListFile;
    private File unlocalisedListFile;
    private List<File> fastaFiles;
    private List<File> flatFiles;
    private List<File> agpFiles;

    public GenomeAssemblyManifest() {
        super(
                // Fields.
                new ArrayList<ManifestFieldDefinition>() {{
                    add(new ManifestFieldDefinition(Fields.NAME, ManifestFieldType.META, 0, 1));
                    add(new ManifestFieldDefinition(Fields.ASSEMBLYNAME, ManifestFieldType.META, 0, 1));
                    add(new ManifestFieldDefinition(Fields.STUDY, ManifestFieldType.META, 1, 1));
                    add(new ManifestFieldDefinition(Fields.SAMPLE, ManifestFieldType.META, 1, 1));
                    add(new ManifestFieldDefinition(Fields.COVERAGE, ManifestFieldType.META, 1, 1));
                    add(new ManifestFieldDefinition(Fields.PROGRAM, ManifestFieldType.META, 1, 1));
                    add(new ManifestFieldDefinition(Fields.PLATFORM, ManifestFieldType.META, 1, 1));
                    add(new ManifestFieldDefinition(Fields.MINGAPLENGTH, ManifestFieldType.META, 0, 1));
                    add(new ManifestFieldDefinition(Fields.MOLECULETYPE, ManifestFieldType.META, 0, 1, Arrays.asList(
                            "genomic DNA",
                            "genomic RNA",
                            "viral cRNA"
                    )));
                    add(new ManifestFieldDefinition(Fields.ASSEMBLY_TYPE, ManifestFieldType.META, 0, 1, Arrays.asList(
                            "clone or isolate",
                            "primary metagenome",
                            "binned metagenome",
                            "Metagenome-Assembled Genome (MAG)",
                            "Environmental Single-Cell Amplified Genome (SAG)"
                    )));
                    add(new ManifestFieldDefinition(Fields.TPA, ManifestFieldType.META, 0, 1, ManifestFieldCv.BOOLEAN_FIELD_VALUES));

                    add(new ManifestFieldDefinition(Fields.CHROMOSOME_LIST, ManifestFieldType.FILE, 0, 1, ManifestFileSuffix.GZIP_OR_BZIP_FILE_SUFFIX));
                    add(new ManifestFieldDefinition(Fields.UNLOCALISED_LIST, ManifestFieldType.FILE, 0, 1, ManifestFileSuffix.GZIP_OR_BZIP_FILE_SUFFIX));
                    add(new ManifestFieldDefinition(Fields.FASTA, ManifestFieldType.FILE, 0, 1, ManifestFileSuffix.FASTA_FILE_SUFFIX));
                    add(new ManifestFieldDefinition(Fields.FLATFILE, ManifestFieldType.FILE, 0, 1, ManifestFileSuffix.GZIP_OR_BZIP_FILE_SUFFIX));
                    add(new ManifestFieldDefinition(Fields.AGP, ManifestFieldType.FILE, 0, 1, ManifestFileSuffix.AGP_FILE_SUFFIX));
                }},

                // File groups.
                new HashSet<List<ManifestFileCount>>() {{
                    // FASTA_WITHOUT_CHROMOSOMES
                    add(new ArrayList<ManifestFileCount>() {{
                        add(new ManifestFileCount(Fields.AGP, 0, null));
                        add(new ManifestFileCount(Fields.FASTA, 1, null));
                        add(new ManifestFileCount(Fields.FLATFILE, 0, null));
                    }});
                    // FASTA_WITH_CHROMOSOMES
                    add(new ArrayList<ManifestFileCount>() {{
                        add(new ManifestFileCount(Fields.AGP, 0, null));
                        add(new ManifestFileCount(Fields.FASTA, 1, null));
                        add(new ManifestFileCount(Fields.FLATFILE, 0, null));
                        add(new ManifestFileCount(Fields.CHROMOSOME_LIST, 1, 1));
                        add(new ManifestFileCount(Fields.UNLOCALISED_LIST, 0, 1));
                    }});
                    // FLATFILE_WITHOUT_CHROMOSOMES
                    add(new ArrayList<ManifestFileCount>() {{
                        add(new ManifestFileCount(Fields.AGP, 0, null));
                        add(new ManifestFileCount(Fields.FLATFILE, 1, null));
                    }});
                    // FLATFILE_WITH_CHROMOSOMES
                    add(new ArrayList<ManifestFileCount>() {{
                        add(new ManifestFileCount(Fields.AGP, 0, null));
                        add(new ManifestFileCount(Fields.FLATFILE, 1, null));
                        add(new ManifestFileCount(Fields.CHROMOSOME_LIST, 1, 1));
                        add(new ManifestFileCount(Fields.UNLOCALISED_LIST, 0, 1));
                    }});

                }});
    }


    public String getName() {
        return name;
    }

    public String getStudyId() {
        return studyId;
    }

    public String getSampleId() {
        return sampleId;
    }

    public String getCoverage() {
        return coverage;
    }

    public String getProgram() {
        return program;
    }

    public String getPlatform() {
        return platform;
    }

    public Integer getMinGapLength() {
        return minGapLength;
    }

    public String getMoleculeType() {
        return moleculeType;
    }

    public String getAssemblyType() {
        return assemblyType;
    }

    public Boolean getTpa() {
        if (tpa == null) {
            return false;
        }
        return tpa;
    }

    public File getChromosomeListFile() {
        return chromosomeListFile;
    }

    public File getUnlocalisedListFile() {
        return unlocalisedListFile;
    }

    public List<File> getFastaFiles() {
        return fastaFiles;
    }

    public List<File> getFlatFiles() {
        return flatFiles;
    }

    public List<File> getAgpFiles() {
        return agpFiles;
    }

    @Override
    public void
    processManifest() {

        name = getResult().getValue(Fields.NAME);
        if (StringUtils.isBlank(name)) {
            name = getResult().getValue(Fields.ASSEMBLYNAME);
        }
        if (StringUtils.isBlank(name)) {
            error("MANIFEST_MISSING_MANDATORY_FIELD", Fields.NAME + " or " + Fields.ASSEMBLYNAME);
        }

        studyId = getResult().getValue(Fields.STUDY);
        sampleId = getResult().getValue(Fields.SAMPLE);

        coverage = getResult().getValue(Fields.COVERAGE);
        getAndValidatePositiveFloat(getResult().getField(Fields.COVERAGE));

        program = getResult().getValue(Fields.PROGRAM);
        platform = getResult().getValue(Fields.PLATFORM);

        if (getResult().getCount(Fields.MINGAPLENGTH) > 0) {
            minGapLength = getAndValidatePositiveInteger(getResult().getField(Fields.MINGAPLENGTH));
        }

        moleculeType = getResult().getValue(Fields.MOLECULETYPE);
        assemblyType = getResult().getValue(Fields.ASSEMBLY_TYPE);

        if (getResult().getCount(Fields.TPA) > 0 ) {
            tpa = getAndValidateBoolean(getResult().getField(Fields.TPA));
        }

        fastaFiles = getFiles(getInputDir(), getResult(), Fields.FASTA);
        agpFiles = getFiles(getInputDir(), getResult(), Fields.AGP);
        flatFiles = getFiles(getInputDir(), getResult(), Fields.FLATFILE);
        chromosomeListFile = getFile(getInputDir(), getResult().getField(Fields.CHROMOSOME_LIST));
        unlocalisedListFile = getFile(getInputDir(), getResult().getField(Fields.UNLOCALISED_LIST));
    }
}
