/*
 * Copyright 2019 EMBL - European Bioinformatics Institute
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

import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.submission.Context;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile.FileType;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFiles;
import uk.ac.ebi.embl.api.validation.submission.SubmissionOptions;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldDefinition;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileCount;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileSuffix;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.ASCIIFileNameProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.FileSuffixProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.StudyProcessor;

public class 
SequenceAssemblyManifest extends ManifestReader
{
    public interface
    Fields 
    {
        String NAME = "NAME";
        String STUDY = "STUDY";
        String TAB = "TAB";
        String FLATFILE = "FLATFILE";
        String DESCRIPTION = "DESCRIPTION";
    }
    
    
	private SubmissionOptions submissionOptions;
    private String name;
    private String description;

    
    @SuppressWarnings( "serial" ) public
    SequenceAssemblyManifest(StudyProcessor studyProcessor ) {
        super(
                // Fields.
                new ArrayList<ManifestFieldDefinition>() { 
                {
                    add( new ManifestFieldDefinition( Fields.NAME, ManifestFieldType.META, 1, 1 ) );
                    add( new ManifestFieldDefinition( Fields.STUDY, ManifestFieldType.META, 1, 1, studyProcessor ) );
                    add( new ManifestFieldDefinition( Fields.DESCRIPTION,  ManifestFieldType.META, 0, 1 ) );
                    add( new ManifestFieldDefinition( Fields.TAB, ManifestFieldType.FILE, 0, 1,
                                                      new ASCIIFileNameProcessor(), 
                                                      new FileSuffixProcessor( ManifestFileSuffix.TAB_FILE_SUFFIX ) ) );
                    
                    add( new ManifestFieldDefinition( Fields.FLATFILE, ManifestFieldType.FILE, 0, 1,
                                                      new ASCIIFileNameProcessor(), 
                                                      new FileSuffixProcessor( ManifestFileSuffix.GZIP_OR_BZIP_FILE_SUFFIX ) ) );
                } },

                // File groups.
                new HashSet<List<ManifestFileCount>>() {
                {
                    add( new ArrayList<ManifestFileCount>() {
                         {
                             add( new ManifestFileCount( Fields.TAB, 1, 1 ) );
                         } 
                    } );
                    add( new ArrayList<ManifestFileCount>() {
                         {
                             add(new ManifestFileCount( Fields.FLATFILE, 1, 1 ) );
                         } } );
                } } );
    }


    @Override public void
    processManifest() 
    {
    	submissionOptions = new SubmissionOptions();
		SubmissionFiles submissionFiles = new SubmissionFiles();
		AssemblyInfoEntry assemblyInfo = new AssemblyInfoEntry();
		name = getResult().getValue( Fields.NAME );
		description = getResult().getValue( Fields.DESCRIPTION );
		assemblyInfo.setName( name );
		getFiles( getInputDir(), getResult(), Fields.TAB ).forEach( fastaFile-> submissionFiles.addFile( new SubmissionFile( FileType.TSV,fastaFile ) ) );
		getFiles( getInputDir(), getResult(), Fields.FLATFILE ).forEach( flatFile->submissionFiles.addFile( new SubmissionFile( FileType.FLATFILE,flatFile ) ) );
		submissionOptions.assemblyInfoEntry = Optional.of( assemblyInfo );
		submissionOptions.context = Optional.of( Context.sequence );
		submissionOptions.submissionFiles = Optional.of( submissionFiles );
		submissionOptions.isRemote = true;
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
    
    
    public SubmissionOptions 
    getSubmissionOptions() 
    {
		return submissionOptions;
	}
}
