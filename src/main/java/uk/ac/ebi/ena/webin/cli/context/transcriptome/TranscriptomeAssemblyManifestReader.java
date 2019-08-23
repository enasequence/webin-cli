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
package uk.ac.ebi.ena.webin.cli.context.transcriptome;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.manifest.*;
import uk.ac.ebi.ena.webin.cli.manifest.processor.*;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TranscriptomeManifest;

public class
TranscriptomeAssemblyManifestReader extends ManifestReader<TranscriptomeManifest>
{
	public interface
	Field 
	{
		String NAME         = "NAME";
		String ASSEMBLYNAME = "ASSEMBLYNAME";
		String STUDY        = "STUDY";
		String SAMPLE       = "SAMPLE";
        String RUN_REF      = "RUN_REF";
        String ANALYSIS_REF = "ANALYSIS_REF";
		String DESCRIPTION  = "DESCRIPTION";
		String PROGRAM      = "PROGRAM";
		String PLATFORM     = "PLATFORM";
		String TPA          = "TPA";
		String FASTA        = "FASTA";
		String FLATFILE     = "FLATFILE";
		String AUTHORS          = "AUTHORS";
		String ADDRESS          = "ADDRESS";
	}

	
	public interface 
	Description 
	{
		String NAME         = "Unique transcriptome assembly name";
		String ASSEMBLYNAME = "Unique transcriptome assembly name";
		String STUDY        = "Study accession or name";
		String SAMPLE       = "Sample accession or name";
		String RUN_REF      = "Run accession or name comma-separated list";
		String ANALYSIS_REF = "Analysis accession or name comma-separated list";

		String DESCRIPTION  = "Transcriptome assembly description";
		String PROGRAM      = "Assembly program";
		String PLATFORM     = "Sequencing platform";
		String TPA          = "Third party annotation";
		String FASTA = "Fasta file";
		String FLATFILE = "Flat file";
		String AUTHORS          = "Author names, comma-separated list";
		String ADDRESS          = "Author address";
	}

	private final TranscriptomeManifest manifest = new TranscriptomeManifest();

	public TranscriptomeAssemblyManifestReader(
			ManifestReaderParameters parameters,
			MetadataProcessorFactory factory)
	{
		super(parameters,
				// Fields.
				new ManifestFieldDefinition.Builder()
					.meta().optional().requiredInSpreadsheet().name( Field.NAME ).desc( Description.NAME ).and()
					.meta().required().name( Field.STUDY        ).desc( Description.STUDY        ).processor( factory.getStudyProcessor()).and()
					.meta().required().name( Field.SAMPLE       ).desc( Description.SAMPLE       ).processor( factory.getSampleProcessor(), factory.getSampleXmlProcessor()).and()
                    .meta().optional().name( Field.RUN_REF      ).desc( Description.RUN_REF      ).processor( factory.getRunProcessor() ).and()
                    .meta().optional().name( Field.ANALYSIS_REF ).desc( Description.ANALYSIS_REF ).processor( factory.getAnalysisProcessor() ).and()
					.meta().optional().name( Field.DESCRIPTION  ).desc( Description.DESCRIPTION  ).and()
					.meta().required().name( Field.PROGRAM      ).desc( Description.PROGRAM      ).and()
					.meta().required().name( Field.PLATFORM     ).desc( Description.PLATFORM     ).and()
					.file().optional().name( Field.FASTA        ).desc( Description.FASTA        ).processor(getFastaProcessors()).and()
					.file().optional().name( Field.FLATFILE     ).desc( Description.FLATFILE     ).processor(getFlatfileProcessors()).and()
					.meta().optional().notInSpreadsheet().name( Field.ASSEMBLYNAME ).desc( Description.ASSEMBLYNAME ).and()
					.meta().optional().notInSpreadsheet().name( Field.TPA ).desc( Description.TPA ).processor( CVFieldProcessor.CV_BOOLEAN ).and()
					.meta().optional().name( Field.AUTHORS ).desc( Description.AUTHORS ).processor(new AuthorProcessor()).and()
					.meta().optional().name( Field.ADDRESS ).desc( Description.ADDRESS )
					.build()
				,
				// File groups.
				new ManifestFileCount.Builder()
					.group()
					.required(Field.FASTA)
					.and().group()
					.required(Field.FLATFILE)
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

	@Override public void
	processManifest() 
	{
		manifest.setName(getResult().getValue( Field.NAME ));
		
		if( StringUtils.isBlank( manifest.getName() ) )
		{
			manifest.setName(getResult().getValue( Field.ASSEMBLYNAME ));
		}

		if (getParameters().isManifestValidateMandatory()) {
			if( StringUtils.isBlank( manifest.getName() ) )
			{
				error( WebinCliMessage.Manifest.MISSING_MANDATORY_FIELD_ERROR, Field.NAME + " or " + Field.ASSEMBLYNAME );
			}
		}

		manifest.setDescription(getResult().getValue( Field.DESCRIPTION ));
		
		Map<String, String> authorAndAddress = getResult().getNonEmptyValues(Field.AUTHORS, Field.ADDRESS);
		if (!authorAndAddress.isEmpty()) {
			if (authorAndAddress.size() == 2) {
				manifest.setAddress(authorAndAddress.get(Field.ADDRESS));
				manifest.setAuthors(authorAndAddress.get(Field.AUTHORS));
			} else {
				error(WebinCliMessage.Manifest.MISSING_ADDRESS_OR_AUTHOR_ERROR);
			}
		}

		manifest.setProgram( getResult().getValue( Field.PROGRAM ) );
		manifest.setPlatform( getResult().getValue( Field.PLATFORM ) );

		if( getResult().getCount(Field.TPA) > 0 )
		{
			manifest.setTpa( getAndValidateBoolean( getResult().getField(Field.TPA ) ) );
		}

		SubmissionFiles<TranscriptomeManifest.FileType> submissionFiles = manifest.files();

		getFiles( getInputDir(), getResult(), Field.FASTA ).forEach(file-> submissionFiles.add( new SubmissionFile( TranscriptomeManifest.FileType.FASTA, file ) ) );
		getFiles( getInputDir(), getResult(), Field.FLATFILE ).forEach(file-> submissionFiles.add( new SubmissionFile( TranscriptomeManifest.FileType.FLATFILE, file) ) );
	}


	@Override
	public TranscriptomeManifest getManifest() {
		return manifest;
	}
}
