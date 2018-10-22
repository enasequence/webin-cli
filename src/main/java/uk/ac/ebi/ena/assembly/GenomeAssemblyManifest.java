package uk.ac.ebi.ena.assembly;

import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.submission.Context;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile.FileType;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFiles;
import uk.ac.ebi.embl.api.validation.submission.SubmissionOptions;
import uk.ac.ebi.ena.manifest.*;
import uk.ac.ebi.ena.manifest.processor.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

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
	private static final String DEFAULT_MOLECULE_TYPE = "genomic DNA";
	SubmissionOptions submissionOptions;

	private final static String[] CV_MOLECULETYPE = {
			"genomic DNA",
			"genomic RNA",
			"viral cRNA"
	};

	private final static String[] CV_ASSEMBLY_TYPE = {
			"clone or isolate",
			"primary metagenome",
			"binned metagenome",
			"Metagenome-Assembled Genome (MAG)",
			"Environmental Single-Cell Amplified Genome (SAG)"
	};

	public
	GenomeAssemblyManifest(SampleProcessor sampleProcessor, StudyProcessor studyProcessor,SourceFeatureProcessor sourceProcessor ) {
		super(
				// Fields.
				new ArrayList<ManifestFieldDefinition>() {{
					add(new ManifestFieldDefinition(Fields.NAME, ManifestFieldType.META, 0, 1));
					add(new ManifestFieldDefinition(Fields.ASSEMBLYNAME, ManifestFieldType.META, 0, 1));
					add(new ManifestFieldDefinition(Fields.STUDY, ManifestFieldType.META, 1, 1, studyProcessor));
					add(new ManifestFieldDefinition(Fields.SAMPLE, ManifestFieldType.META, 1, 1, sampleProcessor,sourceProcessor));
					add(new ManifestFieldDefinition(Fields.COVERAGE, ManifestFieldType.META, 1, 1));
					add(new ManifestFieldDefinition(Fields.PROGRAM, ManifestFieldType.META, 1, 1));
					add(new ManifestFieldDefinition(Fields.PLATFORM, ManifestFieldType.META, 1, 1));
					add(new ManifestFieldDefinition(Fields.MINGAPLENGTH, ManifestFieldType.META, 0, 1));
					add(new ManifestFieldDefinition(Fields.MOLECULETYPE, ManifestFieldType.META, 0, 1,
							new CVFieldProcessor(CV_MOLECULETYPE)));

					add(new ManifestFieldDefinition(Fields.ASSEMBLY_TYPE, ManifestFieldType.META, 0, 1,
							new CVFieldProcessor(CV_ASSEMBLY_TYPE)));

					add(new ManifestFieldDefinition(Fields.TPA, ManifestFieldType.META, 0, 1,
							CVFieldProcessor.CV_BOOLEAN));

					add(new ManifestFieldDefinition(Fields.CHROMOSOME_LIST, ManifestFieldType.FILE, 0, 1,
							new FileSuffixProcessor( ManifestFileSuffix.GZIP_OR_BZIP_FILE_SUFFIX)));
					add(new ManifestFieldDefinition(Fields.UNLOCALISED_LIST, ManifestFieldType.FILE, 0, 1,
							new FileSuffixProcessor( ManifestFileSuffix.GZIP_OR_BZIP_FILE_SUFFIX)));
					add(new ManifestFieldDefinition(Fields.FASTA, ManifestFieldType.FILE, 0, 1,
							new FileSuffixProcessor( ManifestFileSuffix.FASTA_FILE_SUFFIX)));
					add(new ManifestFieldDefinition(Fields.FLATFILE, ManifestFieldType.FILE, 0, 1,
							new FileSuffixProcessor( ManifestFileSuffix.GZIP_OR_BZIP_FILE_SUFFIX)));
					add(new ManifestFieldDefinition(Fields.AGP, ManifestFieldType.FILE, 0, 1,
							new FileSuffixProcessor( ManifestFileSuffix.AGP_FILE_SUFFIX)));
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




	@Override
	public void
	processManifest() {

		submissionOptions =new SubmissionOptions();
		SubmissionFiles submissionFiles = new SubmissionFiles();
		AssemblyInfoEntry assemblyInfo = new AssemblyInfoEntry();
		name = StringUtils.isBlank(getResult().getValue(Fields.NAME))? getResult().getValue(Fields.ASSEMBLYNAME):getResult().getValue(Fields.NAME);
		if (StringUtils.isBlank(name)) 
		{
			error("MANIFEST_MISSING_MANDATORY_FIELD", Fields.NAME + " or " + Fields.ASSEMBLYNAME);
		}
		if(name!=null)
			assemblyInfo.setName(name);

		assemblyInfo.setPlatform(getResult().getValue(Fields.PLATFORM));
		assemblyInfo.setProgram( getResult().getValue(Fields.PROGRAM));
		assemblyInfo.setMoleculeType(getResult().getValue(Fields.MOLECULETYPE)==null?DEFAULT_MOLECULE_TYPE:getResult().getValue(Fields.MOLECULETYPE));
		getAndValidatePositiveFloat(getResult().getField(Fields.COVERAGE));
		assemblyInfo.setCoverage(getResult().getValue(Fields.COVERAGE));
		if (getResult().getCount(Fields.MINGAPLENGTH) > 0) {
			assemblyInfo.setMinGapLength(getAndValidatePositiveInteger(getResult().getField(Fields.MINGAPLENGTH)));
		}
		assemblyInfo.setAssemblyType(getResult().getValue(Fields.ASSEMBLY_TYPE));
		if (getResult().getCount(Fields.TPA) > 0 ) {
			assemblyInfo.setTpa(getAndValidateBoolean(getResult().getField(Fields.TPA)));
		}
		getFiles(getInputDir(), getResult(), Fields.FASTA).forEach(fastaFile-> submissionFiles.addFile(new SubmissionFile(FileType.FASTA,fastaFile)));
		getFiles(getInputDir(), getResult(), Fields.AGP).forEach(agpFile->submissionFiles.addFile(new SubmissionFile(FileType.AGP,agpFile)));
		getFiles(getInputDir(), getResult(), Fields.FLATFILE).forEach(flatFile->submissionFiles.addFile(new SubmissionFile(FileType.FLATFILE,flatFile)));
		getFiles(getInputDir(), getResult(), Fields.CHROMOSOME_LIST).forEach(chromosomeListFile->submissionFiles.addFile(new SubmissionFile(FileType.CHROMOSOME_LIST,chromosomeListFile)));
		getFiles(getInputDir(), getResult(), Fields.UNLOCALISED_LIST).forEach(unlocalisedListFile->submissionFiles.addFile(new SubmissionFile(FileType.UNLOCALISED_LIST,unlocalisedListFile)));

		submissionOptions.assemblyInfoEntry = Optional.of(assemblyInfo);
		submissionOptions.context =Optional.of(Context.genome);
		submissionOptions.submissionFiles= Optional.of(submissionFiles);
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public SubmissionOptions getSubmissionOptions() {
		return submissionOptions;
	}

}
