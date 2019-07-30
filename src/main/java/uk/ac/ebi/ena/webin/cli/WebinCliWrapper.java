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
        SAMPLE_XML,
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
