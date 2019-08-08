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

import java.util.Map;

import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldDefinition;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileCount;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileSuffix;
import uk.ac.ebi.ena.webin.cli.manifest.processor.*;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.manifest.SequenceManifest;

public class
SequenceAssemblyManifestReader extends SequenceManifestReader<SequenceManifest>
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
        String AUTHORS          = "AUTHORS";
        String ADDRESS          = "ADDRESS";
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
        String AUTHORS          = "Author names, comma-separated list";
        String ADDRESS          = "Author address";
    }

    private final SequenceManifest manifest = new SequenceManifest();

    public SequenceAssemblyManifestReader(StudyProcessor    studyProcessor,
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
                    .file().optional().name( Field.FLATFILE     ).desc( Description.FLATFILE     ).processor( getFlatfileProcessors() ).and()
                    .meta().optional().name( Field.AUTHORS      ).desc( Description.AUTHORS      ).processor(new AuthorProcessor()).and()
                    .meta().optional().name( Field.ADDRESS      ).desc( Description.ADDRESS      )
                    .build()
                ,
                // File groups.
                new ManifestFileCount.Builder()
                    .group()
                    .required( Field.TAB )
                    .and().group()
                    .required( Field.FLATFILE )
                    .build()
        );

        if ( studyProcessor != null ) {
            studyProcessor.setCallback(study -> manifest.setStudy(study));
        }
        if ( runProcessor != null ) {
            runProcessor.setCallback(run -> manifest.setRun(run));
        }
        if (analysisProcessor != null ) {
            analysisProcessor.setCallback(analysis -> manifest.setAnalysis(analysis));
        }
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
        Map<String, String> authorAndAddress = getResult().getNonEmptyValues(Field.AUTHORS, Field.ADDRESS);
        if (!authorAndAddress.isEmpty()) {
            if (authorAndAddress.size() == 2) {
                manifest.setAddress(authorAndAddress.get(Field.ADDRESS));
                manifest.setAuthors(authorAndAddress.get(Field.AUTHORS));
            } else {
                error(WebinCliMessage.Manifest.MISSING_ADDRESS_OR_AUTHOR_ERROR);
            }
        }
		manifest.setName(getResult().getValue( Field.NAME ));
		manifest.setDescription(getResult().getValue( Field.DESCRIPTION ));

        SubmissionFiles<SequenceManifest.FileType> submissionFiles = manifest.files();

		getFiles( getInputDir(), getResult(), Field.TAB ).forEach(fastaFile-> submissionFiles.add( new SubmissionFile( SequenceManifest.FileType.TAB,fastaFile ) ) );
		getFiles( getInputDir(), getResult(), Field.FLATFILE ).forEach(flatFile->submissionFiles.add( new SubmissionFile( SequenceManifest.FileType.FLATFILE,flatFile ) ) );
    }

    @Override
    public SequenceManifest getManifest() {
        return manifest;
    }
}
