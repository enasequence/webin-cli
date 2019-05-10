package uk.ac.ebi.ena.webin.cli;

import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;

public interface WebinCliWrapper<T extends ManifestReader> {

    /**
     * Get submission context.
     */
    WebinCliContext getContext();

    /**
     * Read manifest.
     */
    void readManifest(WebinCliParameters parameters );

    /**
     * Validate submission.
     * */
    void validate() throws WebinCliException;

    /**
     * Prepare submission bundle.
     * */
    void prepareSubmissionBundle();

    /**
     * Get submission bundle.
     * */
    SubmissionBundle getSubmissionBundle();





}
