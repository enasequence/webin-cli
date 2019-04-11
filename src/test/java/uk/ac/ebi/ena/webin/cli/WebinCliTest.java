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

import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.assembly.GenomeAssemblyManifest;
import uk.ac.ebi.ena.webin.cli.assembly.SequenceAssemblyManifest;
import uk.ac.ebi.ena.webin.cli.assembly.TranscriptomeAssemblyManifest;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsManifest;
import uk.ac.ebi.ena.webin.cli.upload.ASCPService;

public class 
WebinCliTest
{

    private String
    getRawReadsInfoFields()
    {
        return    RawReadsManifest.Field.STUDY             + " SRP052303\n"
                + RawReadsManifest.Field.SAMPLE            + " ERS2554688\n"
                + RawReadsManifest.Field.PLATFORM          + " ILLUMINA\n"
                + RawReadsManifest.Field.INSTRUMENT        + " unspecifieD\n"
                + RawReadsManifest.Field.INSERT_SIZE       + " 1\n"
                + RawReadsManifest.Field.LIBRARY_NAME      + " YOBA LIB\n"
                + RawReadsManifest.Field.LIBRARY_STRATEGY  + " CLONEEND\n"
                + RawReadsManifest.Field.LIBRARY_SOURCE    + " OTHER\n"
                + RawReadsManifest.Field.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                + RawReadsManifest.Field.DESCRIPTION       + " Some reads description\n"
                + RawReadsManifest.Field.NAME              + " " + WebinCliTestUtils.createName() + "\n ";
    }

    private String
    getGenomeManifestFields(String name)
    {
        return    GenomeAssemblyManifest.Field.ASSEMBLYNAME + " " + name + "\n"
                + GenomeAssemblyManifest.Field.COVERAGE     + " 45\n"
                + GenomeAssemblyManifest.Field.PROGRAM      + " assembly\n"
                + GenomeAssemblyManifest.Field.PLATFORM     + " fghgf\n"
                + GenomeAssemblyManifest.Field.MINGAPLENGTH + " 34\n"
                + GenomeAssemblyManifest.Field.MOLECULETYPE + " genomic DNA\n"
                + GenomeAssemblyManifest.Field.SAMPLE       + " SAMN04526268\n"
                + GenomeAssemblyManifest.Field.STUDY        + " PRJEB20083\n"
                + GenomeAssemblyManifest.Field.DESCRIPTION  + " Some genome assembly description\n";
    }

    private String
    getTranscriptomeManifestFields(String name)
    {
        return    TranscriptomeAssemblyManifest.Field.ASSEMBLYNAME + " " + name + "\n"
                + TranscriptomeAssemblyManifest.Field.PROGRAM      + " assembly\n"
                + TranscriptomeAssemblyManifest.Field.PLATFORM     + " fghgf\n"
                + TranscriptomeAssemblyManifest.Field.SAMPLE       + " SAMN04526268\n"
                + TranscriptomeAssemblyManifest.Field.STUDY        + " PRJEB20083\n"
                + TranscriptomeAssemblyManifest.Field.DESCRIPTION  + " Some transcriptome assembly description\n";
    }

    private String
    getSequenceManifestFields(String name)
    {
        return    SequenceAssemblyManifest.Field.NAME  + " " + name + "\n"
                + SequenceAssemblyManifest.Field.STUDY + " PRJEB20083\n"
                + SequenceAssemblyManifest.Field.DESCRIPTION  + " Some sequence assembly description\n";
    }


    private void
    testWebinCli(WebinCliContext context, Path inputDir, Path outputDir, String manifestContents, boolean ascp ) throws Exception
    {
        WebinCliOptions parameters = new WebinCliOptions();
        parameters.context = context.toString();
        parameters.inputDir = inputDir.toString();
        parameters.outputDir = outputDir.toString();
        parameters.manifest = WebinCliTestUtils.createTempFile("manifest.txt", inputDir,
                manifestContents).toAbsolutePath().toString();
        parameters.userName = System.getenv( "webin-cli-username" );
        parameters.password = System.getenv( "webin-cli-password" );
        parameters.test = true;
        parameters.validate = true;
        parameters.submit = true;
        parameters.ascp = ascp;
        WebinCli webinCli = new WebinCli();
        webinCli.execute( webinCli.init( parameters ) );
    }
    
    @Test public void
    testRawReadsSubmissionWithInfo() throws Exception
    {
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();
        Path cram_file = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/rawreads/18045_1#93.cram", inputDir, false );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", inputDir, getRawReadsInfoFields());

        testWebinCli( WebinCliContext.reads,
                      inputDir,
                      outputDir,
                      ManifestReader.Fields.INFO + " " + infofile.getFileName() + "\n" +
                              RawReadsManifest.Field.CRAM + " " + cram_file.getFileName(),
                      false );
    }
    
    
    @Test public void
    testRawReadsSubmissionWithoutInfo() throws Exception
    {
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();
        Path cram_file = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/rawreads/18045_1#93.cram", inputDir, false );

        testWebinCli( WebinCliContext.reads,
                      inputDir,
                      outputDir,
                      RawReadsManifest.Field.CRAM + " " + cram_file.getFileName() + "\n" + getRawReadsInfoFields(),
                      false );
    }


    @Test public void
    testRawReadsSubmissionWithoutInfoAscp() throws Exception
    {
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path cram_file = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/rawreads/18045_1#93.cram", inputDir, false );
        Assert.assertTrue( new ASCPService().isAvailable() );
        testWebinCli( WebinCliContext.reads,
                      inputDir,
                      outputDir,
                      RawReadsManifest.Field.CRAM + " " + cram_file.getFileName() + "\n" + getRawReadsInfoFields(),
                      true );
    }
    
    
    @Test public void
    testGenomeSubmissionWithInfo() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path flatfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/assembly/valid_flatfileforAgp.txt", inputDir, true, ".gz" );
        Path agpfile  = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/assembly/valid_flatfileAgp.txt", inputDir, true, ".agp.gz" );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", inputDir, getGenomeManifestFields(name));

        testWebinCli( WebinCliContext.genome,
                      inputDir,
                      outputDir,
                      GenomeAssemblyManifest.Field.FLATFILE + " " + flatfile.getFileName() + "\n" +
                      GenomeAssemblyManifest.Field.AGP + " " + agpfile.getFileName() + "\n" +
                      ManifestReader.Fields.INFO + " " + infofile.getFileName(),
                      false );
    }
    
    
    @Test public void
    testGenomeSubmissionWithoutInfo() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path flatfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/assembly/valid_flatfileforAgp.txt", inputDir, true, ".gz" );
        Path agpfile  = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/assembly/valid_flatfileAgp.txt", inputDir, true, ".agp.gz" );

        testWebinCli( WebinCliContext.genome,
                      inputDir,
                      outputDir,
                      GenomeAssemblyManifest.Field.FLATFILE + " " + flatfile.getFileName() + "\n" +
                      GenomeAssemblyManifest.Field.AGP + " " + agpfile.getFileName()  + "\n" +
                      getGenomeManifestFields(name),
                      false );
    }

    @Test public void
    testGenomeSubmissionWithError() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path flatfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/assembly/invalid_flatfile.txt", inputDir, true, ".gz" );

        try {
            testWebinCli(WebinCliContext.genome,
                    inputDir,
                    outputDir,
                    GenomeAssemblyManifest.Field.FLATFILE + " " + flatfile.getFileName() + "\n" +
                    getGenomeManifestFields(name),
                    false);
        }
        catch (WebinCliException ex) {
            Assert.assertTrue(ex.getMessage().contains("Submission validation failed because of a user error"));
            Path reportFile = outputDir.resolve("genome").resolve(name).resolve("validate").resolve(flatfile.getFileName().toString() + ".report" );
            Assert.assertTrue(WebinCliTestUtils.readFile(reportFile).contains("ERROR: Invalid ID line format [ line: 1]"));
            return;
        }
        Assert.assertTrue(false);
    }

    @Test public void
    testSequenceSubmissionWithInfo() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path tabfile = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/template/ERT000003-EST.tsv.gz", inputDir, false );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", inputDir, getSequenceManifestFields(name));

        testWebinCli( WebinCliContext.sequence,
                      inputDir,
                      outputDir,
                      SequenceAssemblyManifest.Field.TAB + " " + tabfile.getFileName() + "\n" +
                      ManifestReader.Fields.INFO + " " + infofile.getFileName(),
                      false );
    }
    
    
    @Test public void
    testSequenceSubmissionWithoutInfo() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path tabfile = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/template/ERT000003-EST.tsv.gz", inputDir, false );

        testWebinCli( WebinCliContext.sequence,
                      inputDir,
                      outputDir,
                      SequenceAssemblyManifest.Field.TAB + " " + tabfile.getFileName() + "\n" +
                      getSequenceManifestFields(name),
                      false );
    }


    @Test public void
    testSequenceSubmissionWithError() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path flatfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/assembly/invalid_flatfile.txt", inputDir, true, ".gz" );

        try {
            testWebinCli( WebinCliContext.sequence,
                    inputDir,
                    outputDir,
                    SequenceAssemblyManifest.Field.FLATFILE + " " + flatfile.getFileName() + "\n" +
                    getSequenceManifestFields(name),
                    false );
        }
        catch (WebinCliException ex) {
            Assert.assertTrue(ex.getMessage().contains("Submission validation failed because of a user error"));
            Path reportFile = outputDir.resolve("sequence").resolve(name).resolve("validate").resolve(flatfile.getFileName().toString() + ".report" );
            Assert.assertTrue(WebinCliTestUtils.readFile(reportFile).contains("ERROR: Invalid ID line format [ line: 1]"));
            return;
        }
        Assert.assertTrue(false);
    }

    @Test public void
    testTranscriptomeSubmissionWithInfo() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path fastafile = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/transcriptome/valid_fasta.fasta.gz", inputDir, false );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", inputDir, getTranscriptomeManifestFields(name));

        testWebinCli( WebinCliContext.transcriptome,
                      inputDir,
                      outputDir,
                      TranscriptomeAssemblyManifest.Field.FASTA + " " + fastafile.getFileName() + "\n" +
                      ManifestReader.Fields.INFO + " " + infofile.getFileName(),
                      false );
    }


    @Test public void
    testTranscriptomeSubmissionWithoutInfo() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path fastafile = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/transcriptome/valid_fasta.fasta.gz", inputDir, false );

        testWebinCli( WebinCliContext.transcriptome,
                      inputDir,
                      outputDir,
                      TranscriptomeAssemblyManifest.Field.FASTA + " " + fastafile.getFileName() + "\n" +
                      getTranscriptomeManifestFields(name),
                      false );
    }

    @Test public void
    testTranscriptomeSubmissionWithError() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path flatfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/assembly/invalid_flatfile.txt", inputDir, true, ".gz" );

        try {
            testWebinCli(WebinCliContext.transcriptome,
                    inputDir,
                    outputDir,
                    TranscriptomeAssemblyManifest.Field.FLATFILE + " " + flatfile.getFileName() + "\n" +
                    getTranscriptomeManifestFields(name),
                    false);
        }
        catch (WebinCliException ex) {
                Assert.assertTrue(ex.getMessage().contains("Submission validation failed because of a user error"));
                Path reportFile = outputDir.resolve("transcriptome").resolve(name).resolve("validate").resolve(flatfile.getFileName().toString() + ".report" );
                Assert.assertTrue(WebinCliTestUtils.readFile(reportFile).contains("ERROR: Invalid ID line format [ line: 1]"));
                return;
            }
            Assert.assertTrue(false);

    }

    @Test public void
    testGetSafeOutputDir() {
        assertEquals("AaZ", WebinCli.getSafeOutputDir("AaZ")[0]);
        assertEquals("A_AA", WebinCli.getSafeOutputDir("A&AA")[0]);
        assertEquals("A.AA", WebinCli.getSafeOutputDir("A.AA")[0]);
        assertEquals("A-AA", WebinCli.getSafeOutputDir("A-AA")[0]);
        assertEquals("A_AA", WebinCli.getSafeOutputDir("A_____AA")[0]);
        assertEquals("AA", WebinCli.getSafeOutputDir("_____AA")[0]);
        assertEquals("AA", WebinCli.getSafeOutputDir("AA_____")[0]);
        assertEquals("_", WebinCli.getSafeOutputDir("_______")[0]);
        assertEquals(".", WebinCli.getSafeOutputDir(".")[0]);

        assertEquals(".", WebinCli.getSafeOutputDir(".", "E_vermicularis_Canary_Islands_upd")[0]);
        assertEquals("E_vermicularis_Canary_Islands_upd", WebinCli.getSafeOutputDir(".", "E_vermicularis_Canary_Islands_upd")[1]);

        assertEquals("AaZ", WebinCli.getSafeOutputDir("AaZ","AaZ")[0]);
        assertEquals("A.AA", WebinCli.getSafeOutputDir("AaZ","A.AA")[1]);
    }
}
