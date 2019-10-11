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
package uk.ac.ebi.ena.webin.cli.context.genome;

import java.util.ArrayList;
import java.util.Map;

import uk.ac.ebi.ena.webin.cli.manifest.*;
import uk.ac.ebi.ena.webin.cli.manifest.processor.*;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest;

public class
GenomeManifestReader extends ManifestReader<GenomeManifest> {

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
		String STUDY            = "Study accession or name";
		String SAMPLE           = "Sample accession or name";
	    String RUN_REF          = "Run accession or name as a comma-separated list";
	    String ANALYSIS_REF     = "Analysis accession or name as a comma-separated list";
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
		String AUTHORS          = "For submission brokers only. Submitter's names as a comma-separated list";
		String ADDRESS          = "For submission brokers only. Submitter's address";
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
			.group("Sequences in a fasta file.")
			.required(Field.FASTA)
			.build();

	private final GenomeManifest manifest = new GenomeManifest();

	public GenomeManifestReader(
			ManifestReaderParameters parameters,
			MetadataProcessorFactory factory)
	{
		super( parameters,
				// Fields.
				new ManifestFieldDefinition.Builder()
					.meta().required().name( Field.NAME             ).desc( Description.NAME             ).synonym(Field.ASSEMBLYNAME).and()
					.meta().required().name( Field.STUDY            ).desc( Description.STUDY            ).processor( factory.getStudyProcessor() ).and()
					.meta().required().name( Field.SAMPLE           ).desc( Description.SAMPLE           ).processor( factory.getSampleProcessor(), factory.getSampleXmlProcessor()).and()
					.meta().optional().name( Field.ASSEMBLY_TYPE    ).desc( Description.ASSEMBLY_TYPE    ).processor( new CVFieldProcessor( CV_ASSEMBLY_TYPE ) ).and()
					.meta().optional().name( Field.DESCRIPTION      ).desc( Description.DESCRIPTION      ).and()
					.meta().required().name( Field.COVERAGE         ).desc( Description.COVERAGE         ).and()
					.meta().required().name( Field.PROGRAM          ).desc( Description.PROGRAM          ).and()
					.meta().required().name( Field.PLATFORM         ).desc( Description.PLATFORM         ).and()
					.meta().optional().name( Field.MINGAPLENGTH     ).desc( Description.MINGAPLENGTH     ).and()
					.meta().optional().name( Field.MOLECULETYPE     ).desc( Description.MOLECULETYPE     ).processor( new CVFieldProcessor( CV_MOLECULE_TYPE ) ).and()
					.meta().optional().name( Field.RUN_REF          ).desc( Description.RUN_REF          ).processor( factory.getRunProcessor() ).and()
					.meta().optional().name( Field.ANALYSIS_REF     ).desc( Description.ANALYSIS_REF     ).processor( factory.getAnalysisProcessor() ).and()
					.file().optional().name( Field.FASTA            ).desc( Description.FASTA            ).processor( getFastaProcessors() ).and()
					.file().optional().name( Field.FLATFILE         ).desc( Description.FLATFILE         ).processor( getFlatfileProcessors() ).and()
					.file().optional().name( Field.AGP              ).desc( Description.AGP              ).processor( getAgpProcessors() ).and()
					.file().optional().name( Field.CHROMOSOME_LIST  ).desc( Description.CHROMOSOME_LIST  ).processor( getChromosomeListProcessors() ).and()
					.file().optional().name( Field.UNLOCALISED_LIST ).desc( Description.UNLOCALISED_LIST ).processor( getUnlocalisedListProcessors() ).and()
					.meta().optional().name( Field.TPA          ).desc( Description.TPA                  ).processor( CVFieldProcessor.CV_BOOLEAN ).and()
					.meta().optional().name( Field.AUTHORS ).desc( Description.AUTHORS                   ).processor(new AuthorProcessor()).and()
					.meta().optional().name( Field.ADDRESS ).desc( Description.ADDRESS                   )
					.build()
				,
				// File groups.
				new ManifestFileCount.Builder()
					.group("Sequences in a fasta file. No chromosomes. An optional AGP file and an optional annotated flat file.")
					.required(Field.FASTA)
					.optional(Field.AGP)
					.optional(Field.FLATFILE)
					.and()
					.group("Sequences in a fasta file. A list of chromosomes. An optional AGP file, an optional annotated flat file and an optional list of unlocalised sequences.")
					.required(Field.FASTA)
					.required(Field.CHROMOSOME_LIST)
					.optional(Field.UNLOCALISED_LIST)
					.optional(Field.AGP)
					.optional(Field.FLATFILE)
					.and()
					.group("Sequences in an annotated flat file. An optional AGP file.")
					.required(Field.FLATFILE)
					.optional(Field.AGP)
					.and()
					.group("Sequences in an annotated flat file. A list of chromosomes. An optional AGP file and an optional list of unlocalised sequences.")
					.required(Field.FLATFILE)
					.required(Field.CHROMOSOME_LIST)
					.optional(Field.UNLOCALISED_LIST)
					.optional(Field.AGP)
					.build()
		);

		if ( factory.getStudyProcessor() != null ) {
			factory.getStudyProcessor().setCallback(study -> manifest.setStudy(study));
		}
		if ( factory.getSampleProcessor() != null ) {
			factory.getSampleProcessor().setCallback(sample -> manifest.setSample(sample));
		}
		if ( factory.getRunProcessor() != null ) {
			factory.getRunProcessor().setCallback(run -> manifest.setRun(run));
		}
		if (factory.getAnalysisProcessor() != null ) {
			factory.getAnalysisProcessor().setCallback(analysis -> manifest.setAnalysis(analysis));
		}
		if (factory.getSampleXmlProcessor() != null) {
			factory.getSampleXmlProcessor().setCallback(sample -> {
				manifest.getSample().setName(sample.getName());
				manifest.getSample().setAttributes(sample.getAttributes());
			});
		}
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
		Map<String, String> authorAndAddress = getManifestReaderResult().getNonEmptyValues(Field.AUTHORS, Field.ADDRESS);
		if (!authorAndAddress.isEmpty()) {
			if (authorAndAddress.size() == 2) {
				manifest.setAddress(authorAndAddress.get(Field.ADDRESS));
				manifest.setAuthors(authorAndAddress.get(Field.AUTHORS));
			} else {
				error(WebinCliMessage.MANIFEST_READER_MISSING_ADDRESS_OR_AUTHOR_ERROR);
			}
		}

		manifest.setName( getManifestReaderResult().getValue( Field.NAME ));
		manifest.setDescription( getManifestReaderResult().getValue( Field.DESCRIPTION ) );
		manifest.setPlatform( getManifestReaderResult().getValue( Field.PLATFORM ) );
		manifest.setProgram( getManifestReaderResult().getValue( Field.PROGRAM ) );
		manifest.setMoleculeType( getManifestReaderResult().getValue( Field.MOLECULETYPE ) == null ? MOLECULE_TYPE_DEFAULT :  getManifestReaderResult().getValue( Field.MOLECULETYPE ) );
		getAndValidatePositiveFloat( getManifestReaderResult().getField( Field.COVERAGE ) );
		manifest.setCoverage(getManifestReaderResult().getValue( Field.COVERAGE ) );
		
		if( getManifestReaderResult().getCount( Field.MINGAPLENGTH ) > 0 )
		{
			manifest.setMinGapLength( getAndValidatePositiveInteger( getManifestReaderResult().getField( Field.MINGAPLENGTH ) ) );
		}

		manifest.setAssemblyType( getManifestReaderResult().getValue( Field.ASSEMBLY_TYPE ) );
		
		if( getManifestReaderResult().getCount( Field.TPA ) > 0 )
		{
			manifest.setTpa( getAndValidateBoolean( getManifestReaderResult().getField( Field.TPA ) ) );
		}

		SubmissionFiles<GenomeManifest.FileType> submissionFiles = manifest.files();

		getFiles( getInputDir(), getManifestReaderResult(), Field.FASTA ).forEach(fastaFile -> submissionFiles.add( new SubmissionFile( GenomeManifest.FileType.FASTA, fastaFile ) ) );
		getFiles( getInputDir(), getManifestReaderResult(), Field.AGP ).forEach(agpFile -> submissionFiles.add( new SubmissionFile( GenomeManifest.FileType.AGP,agpFile ) ) );
		getFiles( getInputDir(), getManifestReaderResult(), Field.FLATFILE ).forEach(flatFile -> submissionFiles.add( new SubmissionFile( GenomeManifest.FileType.FLATFILE,flatFile ) ) );
		getFiles( getInputDir(), getManifestReaderResult(), Field.CHROMOSOME_LIST ).forEach(chromosomeListFile -> submissionFiles.add( new SubmissionFile( GenomeManifest.FileType.CHROMOSOME_LIST, chromosomeListFile ) ) );
		getFiles( getInputDir(), getManifestReaderResult(), Field.UNLOCALISED_LIST ).forEach(unlocalisedListFile -> submissionFiles.add( new SubmissionFile( GenomeManifest.FileType.UNLOCALISED_LIST, unlocalisedListFile ) ) );

        // "primary metagenome" and "binned metagenome" checks
		if( ASSEMBLY_TYPE_PRIMARY_METAGENOME.equals( getManifestReaderResult().getValue( Field.ASSEMBLY_TYPE ) ) ||
			ASSEMBLY_TYPE_BINNED_METAGENOME.equals( getManifestReaderResult().getValue( Field.ASSEMBLY_TYPE ) ) )
		{
		    if(submissionFiles.get()
					.stream()
					.anyMatch(file -> GenomeManifest.FileType.FASTA != file.getFileType() )) {
				error(WebinCliMessage.MANIFEST_READER_INVALID_FILE_GROUP_ERROR,
						getFileGroupText(PRIMARY_AND_BINNED_METAGENOME_FILE_GROUPS),
						" for assembly types: \"" +
								ASSEMBLY_TYPE_PRIMARY_METAGENOME + "\" and \"" +
								ASSEMBLY_TYPE_BINNED_METAGENOME + "\"");
			}
		}
	}


	@Override
	public GenomeManifest getManifest() {
		return manifest;
	}
}
