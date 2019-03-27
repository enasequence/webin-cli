/*
 * Copyright 2018-2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.assembly;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;

import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.submission.Context;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile.FileType;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFiles;
import uk.ac.ebi.embl.api.validation.submission.SubmissionOptions;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.manifest.*;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldDefinition.Builder;
import uk.ac.ebi.ena.webin.cli.manifest.processor.ASCIIFileNameProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.CVFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.FileSuffixProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.SampleProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.SourceFeatureProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.StudyProcessor;

public class
GenomeAssemblyManifest extends ManifestReader {
	public interface Fields {
		String NAME = "NAME";
		String ASSEMBLYNAME = "ASSEMBLYNAME";
		String STUDY = "STUDY";
		String SAMPLE = "SAMPLE";
		String DESCRIPTION = "DESCRIPTION";
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

	public interface Descriptions {
		String NAME = "Unique genome assembly name";
		String ASSEMBLYNAME = "Unique genome assembly name";
		String STUDY = "Study accession or name";
		String SAMPLE = "Sample accession or name";
		String DESCRIPTION = "Genome assembly description";
		String COVERAGE = "Sequencing coverage";
		String PROGRAM = "Assembly program";
		String PLATFORM = "Sequencing platform";
		String MINGAPLENGTH = "Minimum gap length";
		String MOLECULETYPE = "Molecule type";
		String ASSEMBLY_TYPE = "Assembly type";
		String TPA = "Third party annotation";
		String CHROMOSOME_LIST = "Chromosome list file";
		String UNLOCALISED_LIST = "Unlocalised sequence list file";
		String FASTA = "Fasta file";
		String FLATFILE = "Flat file";
		String AGP = "AGP file";
	}

	private String name;
	private String description;

	private static final String DEFAULT_MOLECULE_TYPE = "genomic DNA";
	private SubmissionOptions submissionOptions;
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

	private enum
	CV_ASSEMBLY_TYPE_ORD {
		CLONE_OR_ISOLATE,
		PRIMARY_METAGENOME,
		BINNED_METAGENOME,
		MAG,
		SAG
	}

	private static String getCvAssemblyType(CV_ASSEMBLY_TYPE_ORD ord) {
		return CV_ASSEMBLY_TYPE[ord.ordinal()];
	}

	@SuppressWarnings("serial")
	public GenomeAssemblyManifest(SampleProcessor sampleProcessor, StudyProcessor studyProcessor, SourceFeatureProcessor sourceProcessor) {
		super(
				// Fields.
				new ArrayList<ManifestFieldDefinition>() {
					{
						add(new Builder().meta().optional().name(Fields.NAME).desc(Descriptions.NAME).build());
						add(new Builder().meta().required().name(Fields.STUDY).desc(Descriptions.STUDY).processor(studyProcessor).build());
						add(new Builder().meta().required().name(Fields.SAMPLE).desc(Descriptions.SAMPLE).processor(sampleProcessor, sourceProcessor).build());
						add(new Builder().meta().optional().name(Fields.DESCRIPTION).desc(Descriptions.DESCRIPTION).build());
						add(new Builder().meta().required().name(Fields.COVERAGE).desc(Descriptions.COVERAGE).build());
						add(new Builder().meta().required().name(Fields.PROGRAM).desc(Descriptions.PROGRAM).build());
						add(new Builder().meta().required().name(Fields.PLATFORM).desc(Descriptions.PLATFORM).build());
						add(new Builder().meta().optional().name(Fields.MINGAPLENGTH).desc(Descriptions.MINGAPLENGTH).build());
						add(new Builder().meta().optional().name(Fields.MOLECULETYPE).desc(Descriptions.MOLECULETYPE).processor(new CVFieldProcessor(CV_MOLECULETYPE)).build());
						add(new Builder().meta().optional().name(Fields.ASSEMBLY_TYPE).desc(Descriptions.ASSEMBLY_TYPE).processor(new CVFieldProcessor(CV_ASSEMBLY_TYPE)).build());
						add(new Builder().file().optional().name(Fields.CHROMOSOME_LIST).desc(Descriptions.CHROMOSOME_LIST).processor(getChromosomeListProcessors()).build());
						add(new Builder().file().optional().name(Fields.UNLOCALISED_LIST).desc(Descriptions.UNLOCALISED_LIST).processor(getUnlocalisedListProcessors()).build());
						add(new Builder().file().optional().name(Fields.FASTA).desc(Descriptions.FASTA).processor(getFastaProcessors()).build());
						add(new Builder().file().optional().name(Fields.FLATFILE).desc(Descriptions.FLATFILE).processor(getFlatfileProcessors()).build());
						add(new Builder().file().optional().name(Fields.AGP).desc(Descriptions.AGP).processor(getAgpProcessors()).build());
						add(new Builder().meta().optional().spreadsheet(false).name(Fields.ASSEMBLYNAME).desc(Descriptions.ASSEMBLYNAME).build());
						add(new Builder().meta().optional().spreadsheet(false).name(Fields.TPA).desc(Descriptions.TPA).processor(CVFieldProcessor.CV_BOOLEAN).build());
					}
				},

				// File groups.
				new HashSet<List<ManifestFileCount>>() {
					{
						// FASTA_WITHOUT_CHROMOSOMES
						add(new ArrayList<ManifestFileCount>() {
							{
								add(new ManifestFileCount(Fields.AGP, 0, null));
								add(new ManifestFileCount(Fields.FASTA, 1, null));
								add(new ManifestFileCount(Fields.FLATFILE, 0, null));
							}
						});

						// FASTA_WITH_CHROMOSOMES
						add(new ArrayList<ManifestFileCount>() {
							{
								add(new ManifestFileCount(Fields.AGP, 0, null));
								add(new ManifestFileCount(Fields.FASTA, 1, null));
								add(new ManifestFileCount(Fields.FLATFILE, 0, null));
								add(new ManifestFileCount(Fields.CHROMOSOME_LIST, 1, 1));
								add(new ManifestFileCount(Fields.UNLOCALISED_LIST, 0, 1));
							}
						});

						// FLATFILE_WITHOUT_CHROMOSOMES
						add(new ArrayList<ManifestFileCount>() {
							{
								add(new ManifestFileCount(Fields.AGP, 0, null));
								add(new ManifestFileCount(Fields.FLATFILE, 1, null));
							}
						});

						// FLATFILE_WITH_CHROMOSOMES
						add(new ArrayList<ManifestFileCount>() {
							{
								add(new ManifestFileCount(Fields.AGP, 0, null));
								add(new ManifestFileCount(Fields.FLATFILE, 1, null));
								add(new ManifestFileCount(Fields.CHROMOSOME_LIST, 1, 1));
								add(new ManifestFileCount(Fields.UNLOCALISED_LIST, 0, 1));
							}
						});
					}
				});
	}

    private static ManifestFieldProcessor[] getChromosomeListProcessors() {
        return new ManifestFieldProcessor[]{
                new ASCIIFileNameProcessor(),
                new FileSuffixProcessor(ManifestFileSuffix.GZIP_OR_BZIP_FILE_SUFFIX)};
    }

	private static ManifestFieldProcessor[] getUnlocalisedListProcessors() {
		return new ManifestFieldProcessor[]{
				new ASCIIFileNameProcessor(),
				new FileSuffixProcessor(ManifestFileSuffix.GZIP_OR_BZIP_FILE_SUFFIX)};
	}

	private static ManifestFieldProcessor[] getFastaProcessors() {
		return new ManifestFieldProcessor[]{
				new ASCIIFileNameProcessor(),
				new FileSuffixProcessor(ManifestFileSuffix.FASTA_FILE_SUFFIX)};
	}

	private static ManifestFieldProcessor[] getFlatfileProcessors() {
        return new ManifestFieldProcessor[]{
                new ASCIIFileNameProcessor(),
                new FileSuffixProcessor(ManifestFileSuffix.GZIP_OR_BZIP_FILE_SUFFIX)};
    }

	private static ManifestFieldProcessor[] getAgpProcessors() {
		return new ManifestFieldProcessor[]{
				new ASCIIFileNameProcessor(),
				new FileSuffixProcessor(ManifestFileSuffix.AGP_FILE_SUFFIX)};
	}


    @SuppressWarnings( "serial" )
    @Override public void
	processManifest() 
	{
		submissionOptions = new SubmissionOptions();
		SubmissionFiles submissionFiles = new SubmissionFiles();
		AssemblyInfoEntry assemblyInfo = new AssemblyInfoEntry();
		name = StringUtils.isBlank( getResult().getValue( Fields.NAME ) ) ? getResult().getValue(Fields.ASSEMBLYNAME ) : getResult().getValue( Fields.NAME );
		if( StringUtils.isBlank( name ) ) 
		{
			error( WebinCliMessage.Manifest.MISSING_MANDATORY_FIELD_ERROR, Fields.NAME + " or " + Fields.ASSEMBLYNAME );
		}
		
		if( name != null )
			assemblyInfo.setName( name );

		description = getResult().getValue( Fields.DESCRIPTION );
		assemblyInfo.setPlatform( getResult().getValue( Fields.PLATFORM ) );
		assemblyInfo.setProgram( getResult().getValue( Fields.PROGRAM ) );
		assemblyInfo.setMoleculeType( getResult().getValue( Fields.MOLECULETYPE ) == null ? DEFAULT_MOLECULE_TYPE :  getResult().getValue( Fields.MOLECULETYPE ) );
		getAndValidatePositiveFloat( getResult().getField( Fields.COVERAGE ) );
		assemblyInfo.setCoverage(getResult().getValue( Fields.COVERAGE ) );
		
		if( getResult().getCount( Fields.MINGAPLENGTH ) > 0 )
		{
			assemblyInfo.setMinGapLength( getAndValidatePositiveInteger( getResult().getField( Fields.MINGAPLENGTH ) ) );
		}
		
		assemblyInfo.setAssemblyType( getResult().getValue( Fields.ASSEMBLY_TYPE ) );
		
		if( getResult().getCount( Fields.TPA ) > 0 )
		{
			assemblyInfo.setTpa( getAndValidateBoolean( getResult().getField( Fields.TPA ) ) );
		}
		
		getFiles( getInputDir(), getResult(), Fields.FASTA ).forEach( fastaFile -> submissionFiles.addFile( new SubmissionFile( FileType.FASTA,fastaFile ) ) );
		getFiles( getInputDir(), getResult(), Fields.AGP ).forEach( agpFile -> submissionFiles.addFile( new SubmissionFile( FileType.AGP,agpFile ) ) );
		getFiles( getInputDir(), getResult(), Fields.FLATFILE ).forEach( flatFile -> submissionFiles.addFile( new SubmissionFile( FileType.FLATFILE,flatFile ) ) );
		getFiles( getInputDir(), getResult(), Fields.CHROMOSOME_LIST ).forEach( chromosomeListFile -> submissionFiles.addFile( new SubmissionFile( FileType.CHROMOSOME_LIST, chromosomeListFile ) ) );
		getFiles( getInputDir(), getResult(), Fields.UNLOCALISED_LIST ).forEach( unlocalisedListFile -> submissionFiles.addFile( new SubmissionFile( FileType.UNLOCALISED_LIST, unlocalisedListFile ) ) );

        // "primary metagenome" and "binned metagenome" checks
		if( getCvAssemblyType(CV_ASSEMBLY_TYPE_ORD.PRIMARY_METAGENOME).equals( getResult().getValue( Fields.ASSEMBLY_TYPE ) ) ||
			getCvAssemblyType(CV_ASSEMBLY_TYPE_ORD.BINNED_METAGENOME).equals( getResult().getValue( Fields.ASSEMBLY_TYPE ) ) )
		{
		    if(submissionFiles.getFiles()
					.stream()
					.anyMatch(file -> FileType.FASTA != file.getFileType() )) {
				error(WebinCliMessage.Manifest.INVALID_FILE_GROUP_ERROR,
						getExpectedFileTypeList(new HashSet<List<ManifestFileCount>>() {
							{
								// FASTA ONLY
								add(new ArrayList<ManifestFileCount>() {
									{
										add(new ManifestFileCount(Fields.FASTA, 1, 1));
									}
								});
							}
						}),
						" for assembly types: \"" +
								CV_ASSEMBLY_TYPE[CV_ASSEMBLY_TYPE_ORD.PRIMARY_METAGENOME.ordinal()] + "\" and \"" +
								CV_ASSEMBLY_TYPE[CV_ASSEMBLY_TYPE_ORD.BINNED_METAGENOME.ordinal()] + "\"");
			}
		}
	
		submissionOptions.assemblyInfoEntry = Optional.of( assemblyInfo );
		submissionOptions.context = Optional.of( Context.genome );
		submissionOptions.submissionFiles = Optional.of( submissionFiles );
		submissionOptions.isRemote = true;
	}

	
	public String 
	getName() 
	{
		return name;
	}
	
	
	public SubmissionOptions 
	getSubmissionOptions()
	{
		return submissionOptions;
	}


    @Override public String 
    getDescription()
    {
        return description;
    }
}
