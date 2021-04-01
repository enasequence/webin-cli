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
package uk.ac.ebi.ena.webin.cli.context.taxrefset;

import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldDefinition;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileCount;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileSuffix;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.ASCIIFileNameProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.CustomFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.FileSuffixProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TaxRefSetManifest;

public class TaxRefSetManifestReader extends ManifestReader<TaxRefSetManifest> {

    private final TaxRefSetManifest manifest = new TaxRefSetManifest();
    private static CustomFieldProcessor customFieldProcessor;

    public interface
    Field
    {
        String NAME                     = "NAME";
        String STUDY                    = "STUDY";
        String DESCRIPTION              = "DESCRIPTION";
        String TAXONOMY_SYSTEM          = "TAXONOMY_SYSTEM";
        String TAXONOMY_SYSTEM_VERSION  = "TAXONOMY_SYSTEM_VERSION";
        String FASTA                    = "FASTA";
        String TAB                      = "TAB";
        String CUSTOM_FIELD             = "CUSTOM_FIELD";
    }


    public interface
    Description
    {
        String NAME                     = "Unique taxonomy reference set name";
        String STUDY                    = "Study accession or name";
        String DESCRIPTION              = "Taxonomy reference set description";
        String TAXONOMY_SYSTEM          = "Name of taxonomy system or database from which names are drawn. NCBI taxonomy is preferred.";
        String TAXONOMY_SYSTEM_VERSION  = "Version identifier for the taxonomy system or database.";
        String FASTA                    = "Fasta file";
        String TAB                      = "TSV file";
        String CUSTOM_FIELD             = "Custom column in the sequence metadata file. Must be described in format <column name>:<column description>. " +
                "For example, column1:description1. The column names must match exactly the ones used in the sequence metadata file.";

    }

    public TaxRefSetManifestReader(WebinCliParameters parameters,
                                   MetadataProcessorFactory factory) {
        super( parameters,
                // Fields.
                new ManifestFieldDefinition.Builder()
                        .meta().required().name( Field.NAME                 ).desc( Description.NAME                ).and()
                        .meta().required().name( Field.STUDY                ).desc( Description.STUDY               ).processor( factory.getStudyProcessor() ).and()
                        .meta().required().name( Field.DESCRIPTION          ).desc( Description.DESCRIPTION         ).and()
                        .meta().required().name( Field.TAXONOMY_SYSTEM        ).desc( Description.TAXONOMY_SYSTEM       ).and()
                        .meta().optional().name( Field.TAXONOMY_SYSTEM_VERSION ).desc( Description.TAXONOMY_SYSTEM_VERSION).and()
                        .file().required().name( Field.FASTA                ).desc( Description.FASTA               ).processor(getFastaProcessors()).and()
                        .file().required().name( Field.TAB                  ).desc( Description.TAB               ).processor(getTabProcessors()).and()
                        .meta().optional(100).name( Field.CUSTOM_FIELD          ).desc( Description.CUSTOM_FIELD).processor(getCustomFieldProcessor())
                        .build()
                ,
                // File groups.
                new ManifestFileCount.Builder()
                        .group("Both fasta and tab(tsv) files are mandatory.")
                        .required( Field.FASTA)
                        .required(Field.TAB)
                        .build()
        );

        if ( factory.getStudyProcessor() != null ) {
            factory.getStudyProcessor().setCallback(study -> manifest.setStudy(study));
        }

        getCustomFieldProcessor().setCallback(keyVal-> manifest.addCustomField(keyVal.left,keyVal.right));

        if (parameters != null) {
            manifest.setQuick(parameters.isQuick());
        }
    }

    private static CustomFieldProcessor getCustomFieldProcessor() {
        if(customFieldProcessor == null)
            customFieldProcessor = new CustomFieldProcessor();
        return  customFieldProcessor;
    }

    private static ManifestFieldProcessor[] getFastaProcessors() {
        return new ManifestFieldProcessor[]{
                new ASCIIFileNameProcessor(),
                new FileSuffixProcessor(ManifestFileSuffix.FASTA_FILE_SUFFIX)};
    }

    private static ManifestFieldProcessor[] getTabProcessors() {
        return new ManifestFieldProcessor[]{
                new ASCIIFileNameProcessor(),
                new FileSuffixProcessor( ManifestFileSuffix.TAB_FILE_SUFFIX ) };
    }
    @Override
    public TaxRefSetManifest getManifest() {
        return manifest;
    }

    @Override
    protected void processManifest() {
        manifest.setName( getManifestReaderFields().getValue( Field.NAME ));
        manifest.setDescription( getManifestReaderFields().getValue( Field.DESCRIPTION ) );
        manifest.setTaxonomySystem( getManifestReaderFields().getValue( Field.TAXONOMY_SYSTEM ) );
        manifest.setTaxonomySystemVersion( getManifestReaderFields().getValue( Field.TAXONOMY_SYSTEM_VERSION ) );

        SubmissionFiles<TaxRefSetManifest.FileType> submissionFiles = manifest.files();

        getFiles( getInputDir(), getManifestReaderFields(), Field.FASTA ).forEach(fastaFile -> submissionFiles.add( new SubmissionFile( TaxRefSetManifest.FileType.FASTA, fastaFile ) ) );
        getFiles( getInputDir(), getManifestReaderFields(), Field.TAB ).forEach(tsvFile -> submissionFiles.add( new SubmissionFile( TaxRefSetManifest.FileType.TAB, tsvFile ) ) );

    }
}
