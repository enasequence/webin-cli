package uk.ac.ebi.ena.assembly;

import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.submission.Context;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFiles;
import uk.ac.ebi.embl.api.validation.submission.SubmissionOptions;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile.FileType;
import uk.ac.ebi.ena.assembly.GenomeAssemblyManifest.Fields;
import uk.ac.ebi.ena.manifest.*;
import uk.ac.ebi.ena.manifest.processor.FileSuffixProcessor;
import uk.ac.ebi.ena.manifest.processor.StudyProcessor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;

public class SequenceAssemblyManifest extends ManifestReader
{
    public interface
    Fields {
        String NAME = "NAME";
        String STUDY = "STUDY";
        String TAB = "TAB";
        String FLATFILE = "FLATFILE";
    }
	private SubmissionOptions submissionOptions;

    private String name;

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


    @Override
    public void
    processManifest() {
    	submissionOptions =new SubmissionOptions();
		SubmissionFiles submissionFiles = new SubmissionFiles();
		AssemblyInfoEntry assemblyInfo = new AssemblyInfoEntry();
		name =getResult().getValue(Fields.NAME);
		assemblyInfo.setName(name);
		getFiles(getInputDir(), getResult(), Fields.TAB).forEach(fastaFile-> submissionFiles.addFile(new SubmissionFile(FileType.TSV,fastaFile)));
		getFiles(getInputDir(), getResult(), Fields.FLATFILE).forEach(flatFile->submissionFiles.addFile(new SubmissionFile(FileType.FLATFILE,flatFile)));
		submissionOptions.assemblyInfoEntry = Optional.of(assemblyInfo);
		submissionOptions.context =Optional.of(Context.sequence);
		submissionOptions.submissionFiles= Optional.of(submissionFiles);
		submissionOptions.isRemote = true;
    }
    
	public String getName() {
		return name;
	}
	
	public SubmissionOptions getSubmissionOptions() {
		return submissionOptions;
	}
}
