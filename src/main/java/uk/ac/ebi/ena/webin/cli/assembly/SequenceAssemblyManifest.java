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

import java.util.Optional;

import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.submission.Context;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile.FileType;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFiles;
import uk.ac.ebi.embl.api.validation.submission.SubmissionOptions;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldDefinition;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileCount;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileSuffix;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.ASCIIFileNameProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.AnalysisProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.FileSuffixProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.RunProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.StudyProcessor;

public class
SequenceAssemblyManifest extends ManifestReader 
{
    public interface 
    Field 
    {
        String NAME         = "NAME";
        String STUDY        = "STUDY";
        String RUN_REF      = "RUN_REF";
        String ANALYSIS_REF = "ANALYSIS_REF";
        String DESCRIPTION  = "DESCRIPTION";
        String TAB          = "TAB";
        String FLATFILE     = "FLATFILE";
    }

    
    public interface 
    Description 
    {
        String NAME         = "Unique sequence submission name";
        String STUDY        = "Study accession or name";
        String RUN_REF      = "Run accession or name comma-separated list";
        String ANALYSIS_REF = "Analysis accession or name comma-separated list";
        String DESCRIPTION  = "Sequence submission description";
        String TAB          = "Tabulated file";
        String FLATFILE     = "Flat file";
    }

    private SubmissionOptions submissionOptions;
    private String name;
    private String description;

    public
    SequenceAssemblyManifest( StudyProcessor    studyProcessor, 
                              RunProcessor      runProcessor, 
                              AnalysisProcessor analysisProcessor ) 
    {
        super(
                // Fields.
                new ManifestFieldDefinition.Builder()
                    .meta().required().name( Field.NAME         ).desc( Description.NAME         ).and()
                    .meta().required().name( Field.STUDY        ).desc( Description.STUDY        ).processor( studyProcessor ).and()
                    .meta().optional().name( Field.RUN_REF      ).desc( Description.RUN_REF      ).processor( runProcessor ).and()
                    .meta().optional().name( Field.ANALYSIS_REF ).desc( Description.ANALYSIS_REF ).processor( analysisProcessor ).and()
                    .meta().optional().name( Field.DESCRIPTION  ).desc( Description.DESCRIPTION  ).and()
                    .file().optional().name( Field.TAB          ).desc( Description.TAB          ).processor( getTabProcessors() ).and()
                    .file().optional().name( Field.FLATFILE     ).desc( Description.FLATFILE     ).processor( getFlatfileProcessors() ).build()
                ,
                // File groups.
                new ManifestFileCount.Builder()
                    .group()
                    .required( Field.TAB )
                    .and().group()
                    .required( Field.FLATFILE )
                    .build()

        );
    }

    private static ManifestFieldProcessor[] getTabProcessors() {
        return new ManifestFieldProcessor[]{
                new ASCIIFileNameProcessor(),
                new FileSuffixProcessor( ManifestFileSuffix.TAB_FILE_SUFFIX ) };
    }

    private static ManifestFieldProcessor[] getFlatfileProcessors() {
        return new ManifestFieldProcessor[]{
                new ASCIIFileNameProcessor(),
                new FileSuffixProcessor( ManifestFileSuffix.GZIP_OR_BZIP_FILE_SUFFIX ) };
    }


    @Override public void
    processManifest() 
    {
    	submissionOptions = new SubmissionOptions();
		SubmissionFiles submissionFiles = new SubmissionFiles();
		AssemblyInfoEntry assemblyInfo = new AssemblyInfoEntry();
		name = getResult().getValue( Field.NAME );
		description = getResult().getValue( Field.DESCRIPTION );
		assemblyInfo.setName( name );
		getFiles( getInputDir(), getResult(), Field.TAB ).forEach(fastaFile-> submissionFiles.addFile( new SubmissionFile( FileType.TSV,fastaFile ) ) );
		getFiles( getInputDir(), getResult(), Field.FLATFILE ).forEach(flatFile->submissionFiles.addFile( new SubmissionFile( FileType.FLATFILE,flatFile ) ) );
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
