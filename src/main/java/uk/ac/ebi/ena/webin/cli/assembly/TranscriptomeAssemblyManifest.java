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
import uk.ac.ebi.ena.webin.cli.manifest.processor.ASCIIFileNameProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.CVFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.FileSuffixProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.SampleProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.SourceFeatureProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.StudyProcessor;

public class 
TranscriptomeAssemblyManifest extends ManifestReader
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
		String DESCRIPTION = "DESCRIPTION";
	}

	
	private String name;
	private String description;
	private SubmissionOptions submissionOptions; 

	
	@SuppressWarnings( "serial" ) public
	TranscriptomeAssemblyManifest( SampleProcessor sampleProcessor, StudyProcessor studyProcessor, SourceFeatureProcessor sourceProcessor )
	{
		super(
				// Fields.
				new ArrayList<ManifestFieldDefinition>() {
                {
					add( new ManifestFieldDefinition( Fields.NAME,         ManifestFieldType.META, 0, 1 ) );
					add( new ManifestFieldDefinition( Fields.ASSEMBLYNAME, ManifestFieldType.META, 0, 1 ) );
					add( new ManifestFieldDefinition( Fields.DESCRIPTION,  ManifestFieldType.META, 0, 1 ) );
					add( new ManifestFieldDefinition( Fields.STUDY,        ManifestFieldType.META, 1, 1, studyProcessor ) );
					add( new ManifestFieldDefinition( Fields.SAMPLE,       ManifestFieldType.META, 1, 1, sampleProcessor, sourceProcessor ) );
					add( new ManifestFieldDefinition( Fields.PROGRAM,      ManifestFieldType.META, 1, 1 ) );
					add( new ManifestFieldDefinition( Fields.PLATFORM,     ManifestFieldType.META, 1, 1 ) );
					
					add( new ManifestFieldDefinition( Fields.FASTA, ManifestFieldType.FILE, 0, 1,
					                                  new ASCIIFileNameProcessor(), 
					                                  new FileSuffixProcessor( ManifestFileSuffix.FASTA_FILE_SUFFIX ) ) );
					
					add( new ManifestFieldDefinition( Fields.FLATFILE, ManifestFieldType.FILE, 0, 1,
					                                  new ASCIIFileNameProcessor(), 
					                                  new FileSuffixProcessor( ManifestFileSuffix.GZIP_OR_BZIP_FILE_SUFFIX ) ) );
					
					add( new ManifestFieldDefinition( Fields.TPA, ManifestFieldType.META, 0, 1,
                                                      CVFieldProcessor.CV_BOOLEAN ) );
				} },

				// File groups.
				new HashSet<List<ManifestFileCount>>() {
				{
					add( new ArrayList<ManifestFileCount>() {
					     {
					         add( new ManifestFileCount( Fields.FASTA, 1, 1 ) );
					     } 
					} );
					add( new ArrayList<ManifestFileCount>() {
					     {
					         add(new ManifestFileCount( Fields.FLATFILE, 1, 1 ) );
					     } 
					} );
				} } );
	}

	
	@Override public String 
	getName() 
	{
		return name;
	}

	
	@Override public String
	getDescription()
	{
	    return description;
	}
	
	
	@Override public void
	processManifest() 
	{

		name = getResult().getValue( Fields.NAME );
		
		if( StringUtils.isBlank( name ) )
		{
			name = getResult().getValue( Fields.ASSEMBLYNAME );
		}
		
		if( StringUtils.isBlank( name ) ) 
		{
			error( WebinCliMessage.Manifest.MISSING_MANDATORY_FIELD_ERROR, Fields.NAME + " or " + Fields.ASSEMBLYNAME );
		}

		description = getResult().getValue( Fields.DESCRIPTION );
		
		submissionOptions = new SubmissionOptions();
		SubmissionFiles submissionFiles = new SubmissionFiles();
		AssemblyInfoEntry assemblyInfo = new AssemblyInfoEntry();
        assemblyInfo.setName( name );
		assemblyInfo.setProgram( getResult().getValue( Fields.PROGRAM ) );
		assemblyInfo.setPlatform( getResult().getValue( Fields.PLATFORM ) );

		if( getResult().getCount(Fields.TPA) > 0 ) 
		{
			assemblyInfo.setTpa( getAndValidateBoolean( getResult().getField(Fields.TPA ) ) );
		}

		getFiles( getInputDir(), getResult(), Fields.FASTA ).forEach( fastaFile-> submissionFiles.addFile( new SubmissionFile( FileType.FASTA, fastaFile ) ) );
		getFiles( getInputDir(), getResult(), Fields.FLATFILE ).forEach( fastaFile-> submissionFiles.addFile( new SubmissionFile( FileType.FLATFILE, fastaFile ) ) );
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
