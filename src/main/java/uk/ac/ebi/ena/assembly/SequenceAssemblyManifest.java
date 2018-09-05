package uk.ac.ebi.ena.assembly;

import uk.ac.ebi.ena.manifest.*;
import uk.ac.ebi.ena.manifest.processor.FileSuffixProcessor;
import uk.ac.ebi.ena.manifest.processor.StudyProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SequenceAssemblyManifest extends ManifestReader
{
    public interface
    Fields {
        String NAME = "NAME";
        String STUDY = "STUDY";
        String TAB = "TAB";
        String FLATFILE = "FLATFILE";
    }

    private String name;
    private String studyId;

    private File tsvFile;
    private File flatFile;

    public
    SequenceAssemblyManifest(StudyProcessor studyProcessor ) {
        super(
                // Fields.
                new ArrayList<ManifestFieldDefinition>() {{
                    add(new ManifestFieldDefinition(Fields.NAME, ManifestFieldType.META, 1, 1));
                    add(new ManifestFieldDefinition(Fields.STUDY, ManifestFieldType.META, 1, 1, studyProcessor));
                    add(new ManifestFieldDefinition(Fields.TAB, ManifestFieldType.FILE, 0, 1,
                            new FileSuffixProcessor( ManifestFileSuffix.TAB_FILE_SUFFIX)));
                    add(new ManifestFieldDefinition(Fields.FLATFILE, ManifestFieldType.FILE, 0, 1,
                            new FileSuffixProcessor( ManifestFileSuffix.GZIP_OR_BZIP_FILE_SUFFIX)));
                }},

                // File groups.
                new HashSet<List<ManifestFileCount>>() {{
                    add(new ArrayList<ManifestFileCount>() {{
                        add(new ManifestFileCount(Fields.TAB, 1, 1));
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

    public File getTsvFile() {
        return tsvFile;
    }

    public File getFlatFile() {
        return flatFile;
    }

    @Override
    public void
    processManifest() {

        name = getResult().getValue(Fields.NAME);
        studyId = getResult().getValue(Fields.STUDY);

        tsvFile = getFile(getInputDir(), getResult().getField(Fields.TAB));
        flatFile =  getFile(getInputDir(), getResult().getField(Fields.FLATFILE));
    }
}
