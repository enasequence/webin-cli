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
TranscriptomeAssemblyManifest extends ManifestReader {
	public interface Field {
		String NAME = "NAME";
		String ASSEMBLYNAME = "ASSEMBLYNAME";
		String STUDY = "STUDY";
		String SAMPLE = "SAMPLE";
		String DESCRIPTION = "DESCRIPTION";
		String PROGRAM = "PROGRAM";
		String PLATFORM = "PLATFORM";
		String TPA = "TPA";
		String FASTA = "FASTA";
		String FLATFILE = "FLATFILE";
	}

	public interface Description {
		String NAME = "Unique transcriptome assembly name";
		String ASSEMBLYNAME = "Unique transcriptome assembly name";
		String STUDY = "Study accession or name";
		String SAMPLE = "Sample accession or name";
		String DESCRIPTION = "Transcriptome assembly description";
		String PROGRAM = "Assembly program";
		String PLATFORM = "Sequencing platform";
		String TPA = "Third party annotation";
		String FASTA = "Fasta file";
		String FLATFILE = "Flat file";
	}

	private String name;
	private String description;
	private SubmissionOptions submissionOptions;


	@SuppressWarnings("serial")
	public TranscriptomeAssemblyManifest(SampleProcessor sampleProcessor, StudyProcessor studyProcessor, SourceFeatureProcessor sourceProcessor) {
		super(
				// Fields.
				new ArrayList<ManifestFieldDefinition>() {
					{
						add(new Builder().meta().optional().name(Field.NAME).desc(Description.NAME).build());
						add(new Builder().meta().required().name(Field.STUDY).desc(Description.STUDY).processor(studyProcessor).build());
						add(new Builder().meta().required().name(Field.SAMPLE).desc(Description.SAMPLE).processor(sampleProcessor, sourceProcessor).build());
						add(new Builder().meta().optional().name(Field.DESCRIPTION).desc(Description.DESCRIPTION).build());
						add(new Builder().meta().required().name(Field.PROGRAM).desc(Description.PROGRAM).build());
						add(new Builder().meta().required().name(Field.PLATFORM).desc(Description.PLATFORM).build());
						add(new Builder().file().optional().name(Field.FASTA).desc(Description.FASTA).processor(getFastaProcessors()).build());
						add(new Builder().file().optional().name(Field.FLATFILE).desc(Description.FLATFILE).processor(getFlatfileProcessors()).build());
						add(new Builder().meta().optional().spreadsheet(false).name(Field.ASSEMBLYNAME).desc(Description.ASSEMBLYNAME).build());
						add(new Builder().meta().optional().spreadsheet(false).name(Field.TPA).desc(Description.TPA).processor(CVFieldProcessor.CV_BOOLEAN).build());
					}
				},

				// File groups.
				new HashSet<List<ManifestFileCount>>() {
					{
						add(new ArrayList<ManifestFileCount>() {
							{
								add(new ManifestFileCount(Field.FASTA, 1, 1));
							}
						});
						add(new ArrayList<ManifestFileCount>() {
							{
								add(new ManifestFileCount(Field.FLATFILE, 1, 1));
							}
						});
					}
				});
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

	@Override
	public String getName() {
		return name;
	}


	@Override
	public String getDescription() {
		return description;
	}
	
	
	@Override public void
	processManifest() 
	{

		name = getResult().getValue( Field.NAME );
		
		if( StringUtils.isBlank( name ) )
		{
			name = getResult().getValue( Field.ASSEMBLYNAME );
		}
		
		if( StringUtils.isBlank( name ) ) 
		{
			error( WebinCliMessage.Manifest.MISSING_MANDATORY_FIELD_ERROR, Field.NAME + " or " + Field.ASSEMBLYNAME );
		}

		description = getResult().getValue( Field.DESCRIPTION );
		
		submissionOptions = new SubmissionOptions();
		SubmissionFiles submissionFiles = new SubmissionFiles();
		AssemblyInfoEntry assemblyInfo = new AssemblyInfoEntry();
        assemblyInfo.setName( name );
		assemblyInfo.setProgram( getResult().getValue( Field.PROGRAM ) );
		assemblyInfo.setPlatform( getResult().getValue( Field.PLATFORM ) );

		if( getResult().getCount(Field.TPA) > 0 )
		{
			assemblyInfo.setTpa( getAndValidateBoolean( getResult().getField(Field.TPA ) ) );
		}

		getFiles( getInputDir(), getResult(), Field.FASTA ).forEach(fastaFile-> submissionFiles.addFile( new SubmissionFile( FileType.FASTA, fastaFile ) ) );
		getFiles( getInputDir(), getResult(), Field.FLATFILE ).forEach(fastaFile-> submissionFiles.addFile( new SubmissionFile( FileType.FLATFILE, fastaFile ) ) );
		submissionOptions.assemblyInfoEntry = Optional.of( assemblyInfo );
		submissionOptions.context = Optional.of( Context.transcriptome );
		submissionOptions.submissionFiles = Optional.of( submissionFiles );
		submissionOptions.isRemote = true;
	}
	
	
	public SubmissionOptions 
	getSubmissionOptions() 
	{
		return submissionOptions;
	}

}
