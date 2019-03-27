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
    getGenomeManifestInfoFields()
    {
        return    GenomeAssemblyManifest.Field.ASSEMBLYNAME + " " + WebinCliTestUtils.createName() + "\n"
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
    getTranscriptomeManifestFields()
    {
        return    TranscriptomeAssemblyManifest.Field.ASSEMBLYNAME + " " + WebinCliTestUtils.createName() + "\n"
                + TranscriptomeAssemblyManifest.Field.PROGRAM      + " assembly\n"
                + TranscriptomeAssemblyManifest.Field.PLATFORM     + " fghgf\n"
                + TranscriptomeAssemblyManifest.Field.SAMPLE       + " SAMN04526268\n"
                + TranscriptomeAssemblyManifest.Field.STUDY        + " PRJEB20083\n"
                + TranscriptomeAssemblyManifest.Field.DESCRIPTION  + " Some transcriptome assembly description\n";
    }

    private String
    getSequenceManifestFields()
    {
        return    SequenceAssemblyManifest.Field.NAME  + " " + WebinCliTestUtils.createName() + "\n"
                + SequenceAssemblyManifest.Field.STUDY + " PRJEB20083\n"
                + SequenceAssemblyManifest.Field.DESCRIPTION  + " Some sequence assembly description\n";
    }


    private void
    testWebinCli(WebinCliContext context, Path inputDir, String manifestContents, boolean ascp ) throws Exception
    {
        WebinCli.Params parameters = new WebinCli.Params();
        parameters.context = context.toString();
        parameters.inputDir = inputDir.toString();
        parameters.outputDir  = WebinCliTestUtils.createTempDir().getPath();
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
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        Path cram_file = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/rawreads/18045_1#93.cram", input_dir, false );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", input_dir, getRawReadsInfoFields());

        testWebinCli( WebinCliContext.reads,
                      input_dir,
                      ManifestReader.Fields.INFO + " " + infofile.getFileName() + "\n" + RawReadsManifest.Field.CRAM + " " + cram_file.getFileName(),
                      false );
    }
    
    
    @Test public void
    testRawReadsSubmissionWithoutInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        Path cram_file = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/rawreads/18045_1#93.cram", input_dir, false );

        testWebinCli( WebinCliContext.reads,
                      input_dir,
                      RawReadsManifest.Field.CRAM + " " + cram_file.getFileName() + "\n" + getRawReadsInfoFields(),
                      false );
    }


    @Test public void
    testRawReadsSubmissionWithoutInfoAscp() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        Path cram_file = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/rawreads/18045_1#93.cram", input_dir, false );
        Assert.assertTrue( new ASCPService().isAvailable() );
        testWebinCli( WebinCliContext.reads,
                      input_dir,
                      RawReadsManifest.Field.CRAM + " " + cram_file.getFileName() + "\n" + getRawReadsInfoFields(),
                      true );
    }
    
    
    @Test public void
    testGenomeSubmissionWithInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        
        Path flatfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/assembly/valid_flatfileforAgp.txt", input_dir, true, ".gz" );
        Path agpfile  = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/assembly/valid_flatfileAgp.txt", input_dir, true, ".agp.gz" );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", input_dir, getGenomeManifestInfoFields());

        testWebinCli( WebinCliContext.genome,
                      input_dir,
                      ManifestReader.Fields.INFO + " " + infofile.getFileName() + "\n" +
                              GenomeAssemblyManifest.Field.FLATFILE + " " + flatfile.getFileName() + "\n" +
                              GenomeAssemblyManifest.Field.AGP + " " + agpfile.getFileName(),
                      false );
    }
    
    
    @Test public void
    testGenomeSubmissionWithoutInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        
        Path flatfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/assembly/valid_flatfileforAgp.txt", input_dir, true, ".gz" );
        Path agpfile  = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/assembly/valid_flatfileAgp.txt", input_dir, true, ".agp.gz" );

        testWebinCli( WebinCliContext.genome,
                      input_dir,
                      GenomeAssemblyManifest.Field.FLATFILE + " " + flatfile.getFileName() + "\n" +
                              GenomeAssemblyManifest.Field.AGP + " " + agpfile.getFileName()       + "\n" +
                              getGenomeManifestInfoFields(),
                      false );
    }
    
    
    
    @Test public void
    testSequenceSubmissionWithInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        
        Path tabfile = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/template/ERT000003-EST.tsv.gz", input_dir, false );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", input_dir, getSequenceManifestFields());

        testWebinCli( WebinCliContext.sequence,
                      input_dir,
                      ManifestReader.Fields.INFO + " " + infofile.getFileName() + "\n" +
                              SequenceAssemblyManifest.Field.TAB + " " + tabfile.getFileName(),
                      false );
    }
    
    
    @Test public void
    testSequenceSubmissionWithoutInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        
        Path tabfile = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/template/ERT000003-EST.tsv.gz", input_dir, false );

        testWebinCli( WebinCliContext.sequence,
                      input_dir,
                      SequenceAssemblyManifest.Field.TAB + " " + tabfile.getFileName() + "\n" + getSequenceManifestFields(),
                      false );
    }


    @Test public void
    testTranscriptomeSubmissionWithInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();

        Path fastafile = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/transcriptome/valid_fasta.fasta.gz", input_dir, false );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", input_dir, getTranscriptomeManifestFields());

        testWebinCli( WebinCliContext.transcriptome,
                      input_dir,
                      ManifestReader.Fields.INFO + " " + infofile.getFileName() + "\n" +
                              TranscriptomeAssemblyManifest.Field.FASTA + " " + fastafile.getFileName(),
                      false );
    }


    @Test public void
    testTranscriptomeSubmissionWithoutInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();

        Path fastafile = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/transcriptome/valid_fasta.fasta.gz", input_dir, false );

        testWebinCli( WebinCliContext.transcriptome,
                      input_dir,
                      TranscriptomeAssemblyManifest.Field.FASTA + " " + fastafile.getFileName() + "\n" + getTranscriptomeManifestFields(),
                      false );
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
