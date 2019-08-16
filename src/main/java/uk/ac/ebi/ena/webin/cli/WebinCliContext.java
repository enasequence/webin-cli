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

import uk.ac.ebi.ena.webin.cli.assembly.GenomeAssemblyWebinCli;
import uk.ac.ebi.ena.webin.cli.assembly.SequenceAssemblyWebinCli;
import uk.ac.ebi.ena.webin.cli.assembly.TranscriptomeAssemblyWebinCli;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsWebinCli;

public enum
WebinCliContext {
    sequence("Sequence assembly: %s",
            SequenceAssemblyWebinCli.class),

    transcriptome("Transcriptome assembly: %s",
            TranscriptomeAssemblyWebinCli.class),

    genome("Genome assembly: %s",
            GenomeAssemblyWebinCli.class),

    reads("Raw reads: %s",
            RawReadsWebinCli.class);

    private final String xmlTitle;
    private final Class<? extends AbstractWebinCli> validatorClass;

    WebinCliContext(String xmlTitle,
                    Class<? extends AbstractWebinCli> validatorClass) {
        this.xmlTitle = xmlTitle;
        this.validatorClass = validatorClass;
    }

    public String
    getXmlTitle(String name) {
        return String.format(this.xmlTitle, name);
    }

    public Class<? extends AbstractWebinCli>
    getValidatorClass() {
        return this.validatorClass;
    }
}
