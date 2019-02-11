/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.webin.cli;

import java.io.UnsupportedEncodingException;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.ena.WebinCliTestUtils;
import uk.ac.ebi.ena.assembly.GenomeAssemblyManifest;
import uk.ac.ebi.ena.assembly.SequenceAssemblyManifest;
import uk.ac.ebi.ena.assembly.TranscriptomeAssemblyManifest;
import uk.ac.ebi.ena.manifest.ManifestReader;
import uk.ac.ebi.ena.rawreads.RawReadsManifest;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.upload.ASCPService;

import static org.junit.Assert.assertEquals;

public class 
WebinCliTest
{
    String passwd;
    String usernm;
    
    @Before
    public void
    before() throws UnsupportedEncodingException
    {
        usernm = System.getenv( "webin-cli-username" );
        passwd = System.getenv( "webin-cli-password" );
        Assert.assertTrue( "username should not be blank", StringUtils.isNotBlank( usernm ) );
    }

    private String
    getRawReadsInfoFields()
    {
        return    RawReadsManifest.Fields.STUDY             + " SRP052303\n"
                + RawReadsManifest.Fields.SAMPLE            + " ERS2554688\n"
                + RawReadsManifest.Fields.PLATFORM          + " ILLUMINA\n"
                + RawReadsManifest.Fields.INSTRUMENT        + " unspecifieD\n"
                + RawReadsManifest.Fields.INSERT_SIZE       + " 1\n"
                + RawReadsManifest.Fields.LIBRARY_NAME      + " YOBA LIB\n"
                + RawReadsManifest.Fields.LIBRARY_STRATEGY  + " CLONEEND\n"
                + RawReadsManifest.Fields.LIBRARY_SOURCE    + " OTHER\n"
                + RawReadsManifest.Fields.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                + RawReadsManifest.Fields.DESCRIPTION       + " Some reads description\n"
                + RawReadsManifest.Fields.NAME              + " " + WebinCliTestUtils.createName() + "\n ";
    }

    private String
    getGenomeManifestInfoFields()
    {
        return    GenomeAssemblyManifest.Fields.ASSEMBLYNAME + " " + WebinCliTestUtils.createName() + "\n"
                + GenomeAssemblyManifest.Fields.COVERAGE     + " 45\n"
                + GenomeAssemblyManifest.Fields.PROGRAM      + " assembly\n"
                + GenomeAssemblyManifest.Fields.PLATFORM     + " fghgf\n"
                + GenomeAssemblyManifest.Fields.MINGAPLENGTH + " 34\n"
                + GenomeAssemblyManifest.Fields.MOLECULETYPE + " genomic DNA\n"
                + GenomeAssemblyManifest.Fields.SAMPLE       + " SAMN04526268\n"
                + GenomeAssemblyManifest.Fields.STUDY        + " PRJEB20083\n"
                + GenomeAssemblyManifest.Fields.DESCRIPTION  + " Some genome assembly description\n";
    }

    private String
    getTranscriptomeManifestFields()
    {
        return    TranscriptomeAssemblyManifest.Fields.ASSEMBLYNAME + " " + WebinCliTestUtils.createName() + "\n"
                + TranscriptomeAssemblyManifest.Fields.PROGRAM      + " assembly\n"
                + TranscriptomeAssemblyManifest.Fields.PLATFORM     + " fghgf\n"
                + TranscriptomeAssemblyManifest.Fields.SAMPLE       + " SAMN04526268\n"
                + TranscriptomeAssemblyManifest.Fields.STUDY        + " PRJEB20083\n"
                + TranscriptomeAssemblyManifest.Fields.DESCRIPTION  + " Some transcriptome assembly description\n";
    }

    private String
    getSequenceManifestFields()
    {
        return    SequenceAssemblyManifest.Fields.NAME  + " " + WebinCliTestUtils.createName() + "\n"
                + SequenceAssemblyManifest.Fields.STUDY + " PRJEB20083\n"
                + SequenceAssemblyManifest.Fields.DESCRIPTION  + " Some sequence assembly description\n";
    }


    private void
    testWebinCli( ContextE context, Path inputDir, String manifestContents, boolean ascp ) throws Exception 
    {
        WebinCli.Params parameters = new WebinCli.Params();
        parameters.context = context.toString();
        parameters.inputDir = inputDir.toString();
        parameters.outputDir  = WebinCliTestUtils.createTempDir().getPath();
        parameters.manifest = WebinCliTestUtils.createTempFile("manifest.txt", inputDir, false,
                manifestContents).toAbsolutePath().toString();
        parameters.userName = System.getenv( "webin-cli-username" );
        parameters.password = System.getenv( "webin-cli-password" );
        parameters.test = true;
        parameters.validate = true;
        parameters.submit = true;
        parameters.test = true;
        parameters.ascp = ascp;
        WebinCli webinCli = new WebinCli();
        webinCli.init( parameters );
        webinCli.execute();
    }
    
    @Test public void
    testRawReadsSubmissionWithInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        Path cram_file = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/rawreads/18045_1#93.cram", input_dir, false );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", input_dir, false, getRawReadsInfoFields());

        testWebinCli( ContextE.reads, 
                      input_dir,
                      ManifestReader.Fields.INFO + " " + infofile.getFileName() + "\n" + RawReadsManifest.Fields.CRAM + " " + cram_file.getFileName(), 
                      false );
    }
    
    
    @Test public void
    testRawReadsSubmissionWithoutInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        Path cram_file = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/rawreads/18045_1#93.cram", input_dir, false );

        testWebinCli( ContextE.reads, 
                      input_dir,
                      RawReadsManifest.Fields.CRAM + " " + cram_file.getFileName() + "\n" + getRawReadsInfoFields(),
                      false );
    }


    @Test public void
    testRawReadsSubmissionWithoutInfoAscp() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        Path cram_file = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/rawreads/18045_1#93.cram", input_dir, false );
        Assert.assertTrue( new ASCPService().isAvaliable() );
        testWebinCli( ContextE.reads, 
                      input_dir,
                      RawReadsManifest.Fields.CRAM + " " + cram_file.getFileName() + "\n" + getRawReadsInfoFields(),
                      true );
    }
    
    
    @Test public void
    testGenomeSubmissionWithInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        
        Path flatfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/assembly/valid_flatfileforAgp.txt", input_dir, true, ".gz" );
        Path agpfile  = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/assembly/valid_flatfileAgp.txt", input_dir, true, ".agp.gz" );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", input_dir, false, getGenomeManifestInfoFields());

        testWebinCli( ContextE.genome, 
                      input_dir,
                      ManifestReader.Fields.INFO + " " + infofile.getFileName() + "\n" +
                              GenomeAssemblyManifest.Fields.FLATFILE + " " + flatfile.getFileName() + "\n" +
                              GenomeAssemblyManifest.Fields.AGP + " " + agpfile.getFileName(),
                      false );
    }
    
    
    @Test public void
    testGenomeSubmissionWithoutInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        
        Path flatfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/assembly/valid_flatfileforAgp.txt", input_dir, true, ".gz" );
        Path agpfile  = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/assembly/valid_flatfileAgp.txt", input_dir, true, ".agp.gz" );

        testWebinCli( ContextE.genome, 
                      input_dir,
                      GenomeAssemblyManifest.Fields.FLATFILE + " " + flatfile.getFileName() + "\n" +
                              GenomeAssemblyManifest.Fields.AGP + " " + agpfile.getFileName()       + "\n" +
                              getGenomeManifestInfoFields(),
                      false );
    }
    
    
    
    @Test public void
    testSequenceSubmissionWithInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        
        Path tabfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/template/tsvfile/ERT000003-EST.tsv.gz", input_dir, false );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", input_dir, false, getSequenceManifestFields());

        testWebinCli( ContextE.sequence, 
                      input_dir,
                      ManifestReader.Fields.INFO + " " + infofile.getFileName() + "\n" +
                              SequenceAssemblyManifest.Fields.TAB + " " + tabfile.getFileName(),
                      false );
    }
    
    
    @Test public void
    testSequenceSubmissionWithoutInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        
        Path tabfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/template/tsvfile/ERT000003-EST.tsv.gz", input_dir, false );

        testWebinCli( ContextE.sequence, 
                      input_dir,
                      SequenceAssemblyManifest.Fields.TAB + " " + tabfile.getFileName() + "\n" + getSequenceManifestFields(),
                      false );
    }


    @Test public void
    testTranscriptomeSubmissionWithInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();

        Path fastafile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/transcriptome/simple_fasta/transcriptome.fasta.gz", input_dir, false );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", input_dir, false, getTranscriptomeManifestFields());

        testWebinCli( ContextE.transcriptome, 
                      input_dir,
                      ManifestReader.Fields.INFO + " " + infofile.getFileName() + "\n" +
                              TranscriptomeAssemblyManifest.Fields.FASTA + " " + fastafile.getFileName(),
                      false );
    }


    @Test public void
    testTranscriptomeSubmissionWithoutInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();

        Path fastafile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/transcriptome/simple_fasta/transcriptome.fasta.gz", input_dir, false );

        testWebinCli( ContextE.transcriptome, 
                      input_dir,
                      TranscriptomeAssemblyManifest.Fields.FASTA + " " + fastafile.getFileName() + "\n" + getTranscriptomeManifestFields(),
                      false );
    }


    @Test public void
    testGetSafeOutputDir() throws Exception
    {
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
