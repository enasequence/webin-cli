/*
 * Copyright 2018-2021 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.context.sequence;

import java.util.Map;

import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldDefinition;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileCount;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileSuffix;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.ASCIIFileNameProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.AuthorProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.FileSuffixProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.manifest.SequenceManifest;

public class
SequenceManifestReader extends ManifestReader<SequenceManifest>
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
        String RUN_REF      = "Run accession or name as a comma-separated list";
        String ANALYSIS_REF = "Analysis accession or name as a comma-separated list";
        String DESCRIPTION  = "Sequence submission description";
        String TAB          = "Tabulated file";
        String FLATFILE     = "Flat file";
        String AUTHORS      = "For submission brokers only. Submitter's names as a comma-separated list";
        String ADDRESS      = "For submission brokers only. Submitter's address";
    }

    private final SequenceManifest manifest = new SequenceManifest();

    public SequenceManifestReader(
            WebinCliParameters parameters,
            MetadataProcessorFactory factory) {
        super(parameters,
                // Fields.
                new ManifestFieldDefinition.Builder()
                    .meta().required().name( Field.NAME         ).desc( Description.NAME         ).and()
                    .meta().required().name( Field.STUDY        ).desc( Description.STUDY        ).processor( factory.getStudyProcessor() ).and()
                    .meta().optional().name( Field.RUN_REF      ).desc( Description.RUN_REF      ).processor( factory.getRunProcessor() ).and()
                    .meta().optional().name( Field.ANALYSIS_REF ).desc( Description.ANALYSIS_REF ).processor( factory.getAnalysisProcessor() ).and()
                    .meta().optional().name( Field.DESCRIPTION  ).desc( Description.DESCRIPTION  ).and()
                    .file().optional().name( Field.TAB          ).desc( Description.TAB          ).processor( getTabProcessors() ).and()
                    .file().optional().name( Field.FLATFILE     ).desc( Description.FLATFILE     ).processor( getFlatfileProcessors() ).and()
                    .meta().optional().name( Field.AUTHORS      ).desc( Description.AUTHORS      ).processor(new AuthorProcessor()).and()
                    .meta().optional().name( Field.ADDRESS      ).desc( Description.ADDRESS      ).and()
                    .meta().optional().name(Fields.SUBMISSION_TOOL).desc(Descriptions.SUBMISSION_TOOL).and()
                    .meta().optional().name(Fields.SUBMISSION_TOOL_VERSION).desc(Descriptions.SUBMISSION_TOOL_VERSION)
                    .build()
                ,
                // File groups.
                new ManifestFileCount.Builder()
                    .group("Annotated sequences in a comma separated file.")
                    .required( Field.TAB )
                    .and()
                    .group("Annotated sequences in a flat file.")
                    .required( Field.FLATFILE )
                    .build()
        );

        if ( factory.getStudyProcessor() != null ) {
            factory.getStudyProcessor().setCallback(study -> manifest.setStudy(study));
        }
        if ( factory.getRunProcessor() != null ) {
            factory.getRunProcessor().setCallback(run -> manifest.setRun(run));
        }
        if (factory.getAnalysisProcessor() != null ) {
            factory.getAnalysisProcessor().setCallback(analysis -> manifest.setAnalysis(analysis));
        }

        if (parameters != null) {
            manifest.setQuick(parameters.isQuick());
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
        Map<String, String> authorAndAddress = getManifestReaderResult().getNonEmptyValues(Field.AUTHORS, Field.ADDRESS);
        if (!authorAndAddress.isEmpty()) {
            if (authorAndAddress.size() == 2) {
                manifest.setAddress(authorAndAddress.get(Field.ADDRESS));
                manifest.setAuthors(authorAndAddress.get(Field.AUTHORS));
            } else {
                error(WebinCliMessage.MANIFEST_READER_MISSING_ADDRESS_OR_AUTHOR_ERROR);
            }
        }
		manifest.setName(getManifestReaderResult().getValue( Field.NAME ));
		manifest.setDescription(getManifestReaderResult().getValue( Field.DESCRIPTION ));

        manifest.setSubmissionTool(getManifestReaderResult().getValue(Fields.SUBMISSION_TOOL));
        manifest.setSubmissionToolVersion(getManifestReaderResult().getValue(Fields.SUBMISSION_TOOL_VERSION));

        manifest.setIgnoreErrors(getWebinCliParameters().isIgnoreErrors());

        SubmissionFiles<SequenceManifest.FileType> submissionFiles = manifest.files();

		getFiles( getInputDir(), getManifestReaderResult(), Field.TAB ).forEach(fastaFile-> submissionFiles.add( new SubmissionFile( SequenceManifest.FileType.TAB,fastaFile ) ) );
		getFiles( getInputDir(), getManifestReaderResult(), Field.FLATFILE ).forEach(flatFile->submissionFiles.add( new SubmissionFile( SequenceManifest.FileType.FLATFILE,flatFile ) ) );
    }

    @Override
    public SequenceManifest getManifest() {
        return manifest;
    }
}
