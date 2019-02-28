package uk.ac.ebi.ena.webin.cli;

import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.assembly.GenomeAssemblyWebinCli;
import uk.ac.ebi.ena.webin.cli.assembly.TranscriptomeAssemblyWebinCli;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsWebinCli;

import static org.junit.Assert.assertEquals;

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
