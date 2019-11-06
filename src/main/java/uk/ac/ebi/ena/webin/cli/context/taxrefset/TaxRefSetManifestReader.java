package uk.ac.ebi.ena.webin.cli.context.taxrefset;

import uk.ac.ebi.ena.webin.cli.manifest.*;
import uk.ac.ebi.ena.webin.cli.manifest.processor.ASCIIFileNameProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.CVFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.FileSuffixProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TaxRefSetManifest;

import java.util.Collection;

public class TaxRefSetManifestReader extends ManifestReader<TaxRefSetManifest> {

    public interface
    Field
    {
        String NAME                 = "NAME";
        String STUDY                = "STUDY";
        String DESCRIPTION          = "DESCRIPTION";
        String LOCALTAXONOMY        = "LOCALTAXONOMY";
        String LOCALTAXONOMYVERSION = "LOCALTAXONOMYVERSION";
        String FASTA                = "FASTA";
        String TAB                  = "TAB";
        String CUSTOMFIELD          = "CUSTOMFIELD";
    }


    public interface
    Description
    {
        String NAME                 = "Unique taxonomy reference set name";
        String STUDY                = "Study accession or name";
        String DESCRIPTION          = "Taxonomy reference set description";
        String LOCALTAXONOMY        = "";
        String LOCALTAXONOMYVERSION = "";
        String FASTA                = "Fasta file";
        String TAB                  = "TSV file";
        String CUSTOMFIELD          = "";

    }

    private final TaxRefSetManifest manifest = new TaxRefSetManifest();

    public TaxRefSetManifestReader(ManifestReaderParameters parameters,
                                   MetadataProcessorFactory factory) {
        super( parameters,
                // Fields.
                new ManifestFieldDefinition.Builder()
                        .meta().required().name( Field.NAME                 ).desc( Description.NAME                ).and()
                        .meta().required().name( Field.STUDY                ).desc( Description.STUDY               ).processor( factory.getStudyProcessor() ).and()
                        .meta().required().name( Field.DESCRIPTION          ).desc( Description.DESCRIPTION         ).and()
                        .meta().required().name( Field.LOCALTAXONOMY        ).desc( Description.LOCALTAXONOMY       ).and()
                        .meta().optional().name( Field.LOCALTAXONOMYVERSION ).desc( Description.LOCALTAXONOMYVERSION).and()
                        .file().required().name( Field.FASTA                ).desc( Description.FASTA               ).processor(getFastaProcessors()).and()
                        .file().required().name( Field.TAB                  ).desc( Description.TAB               ).processor(getTabProcessors()).and()
                        .meta().optional().name( Field.CUSTOMFIELD          ).desc( Description.CUSTOMFIELD).processor( CVFieldProcessor.CV_BOOLEAN )
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
        manifest.setName( getManifestReaderResult().getValue( Field.NAME ));
        manifest.setDescription( getManifestReaderResult().getValue( Field.DESCRIPTION ) );
        manifest.setLocalTaxonomy( getManifestReaderResult().getValue( Field.LOCALTAXONOMY ) );
        manifest.setLocalTaxonomyMyVersion( getManifestReaderResult().getValue( Field.LOCALTAXONOMYVERSION ) );
        Collection<String> customFields = getManifestReaderResult().getValues( Field.CUSTOMFIELD );
        if (customFields != null && !customFields.isEmpty()) {
            customFields.forEach(cf -> {
                if (cf != null) {
                    String keyVal[] = cf.split(":");
                    manifest.addCustomField(keyVal[0], keyVal.length == 2 ? keyVal[1] : "");
                }
            });
        }

        SubmissionFiles<TaxRefSetManifest.FileType> submissionFiles = manifest.files();

        getFiles( getInputDir(), getManifestReaderResult(), Field.FASTA ).forEach(fastaFile -> submissionFiles.add( new SubmissionFile( TaxRefSetManifest.FileType.FASTA, fastaFile ) ) );
        getFiles( getInputDir(), getManifestReaderResult(), Field.TAB ).forEach(tsvFile -> submissionFiles.add( new SubmissionFile( TaxRefSetManifest.FileType.TAB, tsvFile ) ) );

    }
}
