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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.assembly.GenomeAssemblyWebinCli;
import uk.ac.ebi.ena.webin.cli.assembly.TranscriptomeAssemblyWebinCli;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsWebinCli;

public class AbstractWebinCliTest {

    @Test
    public void
    testGetAlias() {
        GenomeAssemblyWebinCli genomeAssemblyWebinCli = new GenomeAssemblyWebinCli();
        genomeAssemblyWebinCli.setName("TEST_NAME");
        assertEquals("webin-genome-TEST_NAME", genomeAssemblyWebinCli.getAlias());

        TranscriptomeAssemblyWebinCli transcriptomeAssemblyWebinCli = new TranscriptomeAssemblyWebinCli();
        transcriptomeAssemblyWebinCli.setName("TEST_NAME");
        assertEquals("webin-transcriptome-TEST_NAME", transcriptomeAssemblyWebinCli.getAlias());

//        SequenceAssemblyWebinCli sequenceAssemblyWebinCli = new SequenceAssemblyWebinCli();
//        sequenceAssemblyWebinCli.setName("TEST_NAME");
//        assertEquals("webin-sequence-TEST_NAME", sequenceAssemblyWebinCli.getAlias());

        RawReadsWebinCli rawReadsWebinCli = new RawReadsWebinCli();
        rawReadsWebinCli.setName("TEST_NAME");
        assertEquals("webin-reads-TEST_NAME", rawReadsWebinCli.getAlias());
    }
}
