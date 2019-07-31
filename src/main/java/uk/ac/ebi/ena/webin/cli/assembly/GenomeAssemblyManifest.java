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
import java.util.Map;
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
import uk.ac.ebi.ena.webin.cli.manifest.processor.*;

public class
GenomeAssemblyManifest extends ManifestReader {

	public interface
	Field 
	{
		String NAME             = "NAME";
		String ASSEMBLYNAME     = "ASSEMBLYNAME";
		String STUDY            = "STUDY";
		String SAMPLE           = "SAMPLE";
		String RUN_REF          = "RUN_REF";
		String ANALYSIS_REF     = "ANALYSIS_REF";
        String DESCRIPTION      = "DESCRIPTION";
		String COVERAGE         = "COVERAGE";
		String PROGRAM          = "PROGRAM";
		String PLATFORM         = "PLATFORM";
		String MINGAPLENGTH     = "MINGAPLENGTH";
		String MOLECULETYPE     = "MOLECULETYPE";
		String ASSEMBLY_TYPE    = "ASSEMBLY_TYPE";
		String TPA              = "TPA";
		String CHROMOSOME_LIST  = "CHROMOSOME_LIST";
		String UNLOCALISED_LIST = "UNLOCALISED_LIST";
		String FASTA            = "FASTA";
		String FLATFILE         = "FLATFILE";
		String AGP              = "AGP";
		String AUTHORS          = "AUTHORS";
		String ADDRESS          = "ADDRESS";
	}

	
	public interface
	Description 
	{
		String NAME             = "Unique genome assembly name";
		String ASSEMBLYNAME     = "Unique genome assembly name";
		String STUDY            = "Study accession or name";
		String SAMPLE           = "Sample accession or name";
	    String RUN_REF          = "Run accession or name comma-separated list";
	    String ANALYSIS_REF     = "Analysis accession or name comma-separated list";
		String DESCRIPTION      = "Genome assembly description";
		String COVERAGE         = "Sequencing coverage";
		String PROGRAM          = "Assembly program";
		String PLATFORM         = "Sequencing platform";
		String MINGAPLENGTH     = "Minimum gap length";
		String MOLECULETYPE     = "Molecule type";
		String ASSEMBLY_TYPE    = "Assembly type";
		String TPA              = "Third party annotation";
		String CHROMOSOME_LIST  = "Chromosome list file";
		String UNLOCALISED_LIST = "Unlocalised sequence list file";
		String FASTA            = "Fasta file";
		String FLATFILE         = "Flat file";
		String AGP              = "AGP file";
		String AUTHORS          = "Author names, comma-separated list";
		String ADDRESS          = "Author address";
	}

	
	private static final String MOLECULE_TYPE_DEFAULT = "genomic DNA";
	private static final String ASSEMBLY_TYPE_PRIMARY_METAGENOME = "primary metagenome";
	private static final String ASSEMBLY_TYPE_BINNED_METAGENOME = "binned metagenome";

	private static final ManifestCVList CV_MOLECULE_TYPE = new ManifestCVList(
			"genomic DNA",
			"genomic RNA",
			"viral cRNA"
	);

	private static final ManifestCVList CV_ASSEMBLY_TYPE = new ManifestCVList(
			"clone or isolate",
			ASSEMBLY_TYPE_PRIMARY_METAGENOME,
			ASSEMBLY_TYPE_BINNED_METAGENOME,
			"Metagenome-Assembled Genome (MAG)",
			"Environmental Single-Cell Amplified Genome (SAG)"
	);

	public static final ArrayList<ManifestFileGroup> PRIMARY_AND_BINNED_METAGENOME_FILE_GROUPS = new ManifestFileCount.Builder()
			.group()
			.required(Field.FASTA)
			.build();

	private String name;
	private String description;
	private SubmissionOptions submissionOptions;

	public 
	GenomeAssemblyManifest( SampleProcessor   sampleProcessor, 
	                        StudyProcessor    studyProcessor, 
	                        RunProcessor      runProcessor, 
	                        AnalysisProcessor analysisProcessor,
	                        SampleXmlProcessor sampleXmlProcessor)
	{
		super(
				// Fields.
				new ManifestFieldDefinition.Builder()
					.meta().optional().requiredInSpreadsheet().name( Field.NAME ).desc( Description.NAME ).and()
					.meta().required().name( Field.STUDY            ).desc( Description.STUDY            ).processor( studyProcessor ).and()
					.meta().required().name( Field.SAMPLE           ).desc( Description.SAMPLE           ).processor( sampleProcessor, sampleXmlProcessor).and()
					.meta().optional().name( Field.RUN_REF          ).desc( Description.RUN_REF          ).processor( runProcessor ).and()
					.meta().optional().name( Field.ANALYSIS_REF     ).desc( Description.ANALYSIS_REF     ).processor( analysisProcessor ).and()
					.meta().optional().name( Field.DESCRIPTION      ).desc( Description.DESCRIPTION      ).and()
					.meta().required().name( Field.COVERAGE         ).desc( Description.COVERAGE         ).and()
					.meta().required().name( Field.PROGRAM          ).desc( Description.PROGRAM          ).and()
					.meta().required().name( Field.PLATFORM         ).desc( Description.PLATFORM         ).and()
					.meta().optional().name( Field.MINGAPLENGTH     ).desc( Description.MINGAPLENGTH     ).and()
					.meta().optional().name( Field.MOLECULETYPE     ).desc( Description.MOLECULETYPE     ).processor( new CVFieldProcessor( CV_MOLECULE_TYPE ) ).and()
					.meta().optional().name( Field.ASSEMBLY_TYPE    ).desc( Description.ASSEMBLY_TYPE    ).processor( new CVFieldProcessor( CV_ASSEMBLY_TYPE ) ).and()
					.file().optional().name( Field.CHROMOSOME_LIST  ).desc( Description.CHROMOSOME_LIST  ).processor( getChromosomeListProcessors() ).and()
					.file().optional().name( Field.UNLOCALISED_LIST ).desc( Description.UNLOCALISED_LIST ).processor( getUnlocalisedListProcessors() ).and()
					.file().optional().name( Field.FASTA            ).desc( Description.FASTA            ).processor( getFastaProcessors() ).and()
					.file().optional().name( Field.FLATFILE         ).desc( Description.FLATFILE         ).processor( getFlatfileProcessors() ).and()
					.file().optional().name( Field.AGP              ).desc( Description.AGP              ).processor( getAgpProcessors() ).and()
					.meta().optional().notInSpreadsheet().name( Field.ASSEMBLYNAME ).desc( Description.ASSEMBLYNAME ).and()
					.meta().optional().notInSpreadsheet().name( Field.TPA          ).desc( Description.TPA          ).processor( CVFieldProcessor.CV_BOOLEAN ).and()
					.meta().optional().name( Field.AUTHORS ).desc( Description.AUTHORS ).processor(new AuthorProcessor()).and()
					.meta().optional().name( Field.ADDRESS ).desc( Description.ADDRESS )
					.build()
				,
				// File groups.
				new ManifestFileCount.Builder()
					.group()
					.required(Field.FASTA)
					.optional(Field.AGP)
					.optional(Field.FLATFILE)
					.and().group()
					.required(Field.FASTA)
					.required(Field.CHROMOSOME_LIST)
					.optional(Field.UNLOCALISED_LIST)
					.optional(Field.AGP)
					.optional(Field.FLATFILE)
					.and().group()
					.required(Field.FLATFILE)
					.optional(Field.AGP)
					.and().group()
					.required(Field.FLATFILE)
					.required(Field.CHROMOSOME_LIST)
					.optional(Field.UNLOCALISED_LIST)
					.optional(Field.AGP)
					.build()
		);
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


    @Override public void
	processManifest() 
	{
		submissionOptions = new SubmissionOptions();
		SubmissionFiles submissionFiles = new SubmissionFiles();
		AssemblyInfoEntry assemblyInfo = new AssemblyInfoEntry();
		name = StringUtils.isBlank( getResult().getValue( Field.NAME ) ) ? getResult().getValue(Field.ASSEMBLYNAME ) : getResult().getValue( Field.NAME );
		if( StringUtils.isBlank( name ) ) 
		{
			error( WebinCliMessage.Manifest.MISSING_MANDATORY_FIELD_ERROR, Field.NAME + " or " + Field.ASSEMBLYNAME );
		}

		Map<String, String> authorAndAddress = getResult().getNonEmptyValues(Field.AUTHORS, Field.ADDRESS);
		if (!authorAndAddress.isEmpty()) {
			if (authorAndAddress.size() == 2) {
				assemblyInfo.setAddress(authorAndAddress.get(Field.ADDRESS));
				assemblyInfo.setAuthors(authorAndAddress.get(Field.AUTHORS));
			} else {
				error(WebinCliMessage.Manifest.MISSING_ADDRESS_OR_AUTHOR_ERROR);
			}
		}

		if( name != null )
			assemblyInfo.setName( name );

		description = getResult().getValue( Field.DESCRIPTION );
		assemblyInfo.setPlatform( getResult().getValue( Field.PLATFORM ) );
		assemblyInfo.setProgram( getResult().getValue( Field.PROGRAM ) );
		assemblyInfo.setMoleculeType( getResult().getValue( Field.MOLECULETYPE ) == null ? MOLECULE_TYPE_DEFAULT :  getResult().getValue( Field.MOLECULETYPE ) );
		getAndValidatePositiveFloat( getResult().getField( Field.COVERAGE ) );
		assemblyInfo.setCoverage(getResult().getValue( Field.COVERAGE ) );
		
		if( getResult().getCount( Field.MINGAPLENGTH ) > 0 )
		{
			assemblyInfo.setMinGapLength( getAndValidatePositiveInteger( getResult().getField( Field.MINGAPLENGTH ) ) );
		}
		
		assemblyInfo.setAssemblyType( getResult().getValue( Field.ASSEMBLY_TYPE ) );
		
		if( getResult().getCount( Field.TPA ) > 0 )
		{
			assemblyInfo.setTpa( getAndValidateBoolean( getResult().getField( Field.TPA ) ) );
		}
		
		getFiles( getInputDir(), getResult(), Field.FASTA ).forEach(fastaFile -> submissionFiles.addFile( new SubmissionFile( FileType.FASTA,fastaFile ) ) );
		getFiles( getInputDir(), getResult(), Field.AGP ).forEach(agpFile -> submissionFiles.addFile( new SubmissionFile( FileType.AGP,agpFile ) ) );
		getFiles( getInputDir(), getResult(), Field.FLATFILE ).forEach(flatFile -> submissionFiles.addFile( new SubmissionFile( FileType.FLATFILE,flatFile ) ) );
		getFiles( getInputDir(), getResult(), Field.CHROMOSOME_LIST ).forEach(chromosomeListFile -> submissionFiles.addFile( new SubmissionFile( FileType.CHROMOSOME_LIST, chromosomeListFile ) ) );
		getFiles( getInputDir(), getResult(), Field.UNLOCALISED_LIST ).forEach(unlocalisedListFile -> submissionFiles.addFile( new SubmissionFile( FileType.UNLOCALISED_LIST, unlocalisedListFile ) ) );

        // "primary metagenome" and "binned metagenome" checks
		if( ASSEMBLY_TYPE_PRIMARY_METAGENOME.equals( getResult().getValue( Field.ASSEMBLY_TYPE ) ) ||
			ASSEMBLY_TYPE_BINNED_METAGENOME.equals( getResult().getValue( Field.ASSEMBLY_TYPE ) ) )
		{
		    if(submissionFiles.getFiles()
					.stream()
					.anyMatch(file -> FileType.FASTA != file.getFileType() )) {
				error(WebinCliMessage.Manifest.INVALID_FILE_GROUP_ERROR,
						getFileGroupText(PRIMARY_AND_BINNED_METAGENOME_FILE_GROUPS),
						" for assembly types: \"" +
								ASSEMBLY_TYPE_PRIMARY_METAGENOME + "\" and \"" +
								ASSEMBLY_TYPE_BINNED_METAGENOME + "\"");
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
