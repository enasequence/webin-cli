package uk.ac.ebi.ena.assembly;

import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.ena.manifest.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SequenceManifest extends ManifestReader
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

    public SequenceManifest() {
        super(
                // Fields.
                new ArrayList<ManifestFieldDefinition>() {{
                    add(new ManifestFieldDefinition(Fields.NAME, ManifestFieldType.META, 1, 1));
                    add(new ManifestFieldDefinition(Fields.STUDY, ManifestFieldType.META, 1, 1));
                    add(new ManifestFieldDefinition(Fields.TAB, ManifestFieldType.FILE, 0, 1, ManifestFileSuffix.TAB_FILE_SUFFIX));
                    add(new ManifestFieldDefinition(Fields.FLATFILE, ManifestFieldType.FILE, 0, 1, ManifestFileSuffix.GZIP_OR_BZIP_FILE_SUFFIX));
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
