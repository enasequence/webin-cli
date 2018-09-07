package uk.ac.ebi.ena.assembly;

import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.ena.manifest.*;
import uk.ac.ebi.ena.manifest.processor.CVFieldProcessor;
import uk.ac.ebi.ena.manifest.processor.FileSuffixProcessor;
import uk.ac.ebi.ena.manifest.processor.SampleProcessor;
import uk.ac.ebi.ena.manifest.processor.StudyProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TranscriptomeAssemblyManifest extends ManifestReader
{
    public interface
    Fields {
        String NAME = "NAME";
        String ASSEMBLYNAME = "ASSEMBLYNAME";
        String STUDY = "STUDY";
        String SAMPLE = "SAMPLE";
        String PROGRAM = "PROGRAM";
        String PLATFORM = "PLATFORM";
        String FASTA = "FASTA";
        String FLATFILE = "FLATFILE";
        String TPA = "TPA";
   }

    private String name;
    private String studyId;
    private String sampleId;
    private String program;
    private String platform;
    private Boolean tpa;

    private File fastaFile;
    private File flatFile;

    public
    TranscriptomeAssemblyManifest(SampleProcessor sampleProcessor, StudyProcessor studyProcessor ) {
        super(
                // Fields.
                new ArrayList<ManifestFieldDefinition>() {{
                    add(new ManifestFieldDefinition(Fields.NAME, ManifestFieldType.META, 0, 1));
                    add(new ManifestFieldDefinition(Fields.ASSEMBLYNAME, ManifestFieldType.META, 0, 1));
                    add(new ManifestFieldDefinition(Fields.STUDY, ManifestFieldType.META, 1, 1, studyProcessor));
                    add(new ManifestFieldDefinition(Fields.SAMPLE, ManifestFieldType.META, 1, 1, sampleProcessor));
                    add(new ManifestFieldDefinition(Fields.PROGRAM, ManifestFieldType.META, 1, 1));
                    add(new ManifestFieldDefinition(Fields.PLATFORM, ManifestFieldType.META, 1, 1));
                    add(new ManifestFieldDefinition(Fields.FASTA, ManifestFieldType.FILE, 0, 1,
                            new FileSuffixProcessor( ManifestFileSuffix.FASTA_FILE_SUFFIX)));
                    add(new ManifestFieldDefinition(Fields.FLATFILE, ManifestFieldType.FILE, 0, 1,
                            new FileSuffixProcessor( ManifestFileSuffix.GZIP_OR_BZIP_FILE_SUFFIX)));
                    add(new ManifestFieldDefinition(Fields.TPA, ManifestFieldType.META, 0, 1,
                            CVFieldProcessor.CV_BOOLEAN));
                }},

                // File groups.
                new HashSet<List<ManifestFileCount>>() {{
                    add(new ArrayList<ManifestFileCount>() {{
                        add(new ManifestFileCount(Fields.FASTA, 1, 1));
                    }});
                    add(new ArrayList<ManifestFileCount>() {{
                        add(new ManifestFileCount(Fields.FLATFILE, 1, 1));
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

    public String getProgram() {
        return program;
    }

    public String getPlatform() {
        return platform;
    }

    public Boolean getTpa() {
        if (tpa == null) {
            return false;
        }
        return tpa;
    }

    public File getFastaFile() {
        return fastaFile;
    }

    public File getFlatFile() {
        return flatFile;
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

        program = getResult().getValue(Fields.PROGRAM);
        platform = getResult().getValue(Fields.PLATFORM);

        if (getResult().getCount(Fields.TPA) > 0 ) {
            tpa = getAndValidateBoolean(getResult().getField(Fields.TPA));
        }

        fastaFile = getFile(getInputDir(), getResult().getField(Fields.FASTA));
        flatFile = getFile(getInputDir(), getResult().getField(Fields.FLATFILE));
    }
}
