package uk.ac.ebi.ena.webin.cli;

import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;

public interface WebinCliWrapper<T extends ManifestReader> {

    /**
     * Services used by retrieve metadata from external sources.
     */
    enum MetadataService {
        SAMPLE,
        STUDY,
        SOURCE,
        RUN,
        ANALYSIS
    }

    /**
     * Check if a metadata service is active.
     */
    boolean isMetadataServiceActive(AbstractWebinCli.MetadataService service);

    /**
     * Enable or disable a metadata service.
     */
    void setMetadataServiceActive(AbstractWebinCli.MetadataService service, boolean isActive);

    /**
     * Enable or disable all metadata services.
     */
    void setMetadataServiceActive(boolean isActive);

    /**
     * Get the submission context.
     */
    WebinCliContext getContext();

    /**
     * Read the submission manifest.
     */
    void readManifest(WebinCliParameters parameters );

    /**
     * Validate the submission.
     * */
    void validate() throws WebinCliException;

    /**
     * Prepare the submission bundle.
     * */
    void prepareSubmissionBundle();

    /**
     * Get the submission bundle.
     * */
    SubmissionBundle getSubmissionBundle();





}
