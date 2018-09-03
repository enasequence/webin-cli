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

import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.ena.WebinCliTestUtils;
import uk.ac.ebi.ena.assembly.GenomeManifest;
import uk.ac.ebi.ena.assembly.SequenceManifest;
import uk.ac.ebi.ena.assembly.TranscriptomeManifest;
import uk.ac.ebi.ena.manifest.ManifestReader;
import uk.ac.ebi.ena.rawreads.RawReadsManifest;
import uk.ac.ebi.ena.submit.ContextE;

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
                + RawReadsManifest.Fields.NAME              + " " + WebinCliTestUtils.createName() + "\n ";
    }

    private String
    getGenomeManifestInfoFields()
    {
        return    GenomeManifest.Fields.ASSEMBLYNAME + " " + WebinCliTestUtils.createName() + "\n"
                + GenomeManifest.Fields.COVERAGE     + " 45\n"
                + GenomeManifest.Fields.PROGRAM      + " assembly\n"
                + GenomeManifest.Fields.PLATFORM     + " fghgf\n"
                + GenomeManifest.Fields.MINGAPLENGTH + " 34\n"
                + GenomeManifest.Fields.MOLECULETYPE + " genomic DNA\n"
                + GenomeManifest.Fields.SAMPLE       + " SAMN04526268\n"
                + GenomeManifest.Fields.STUDY        + " PRJEB20083\n";
    }

    private String
    getTranscriptomeManifestFields()
    {
        return    TranscriptomeManifest.Fields.ASSEMBLYNAME + " " + WebinCliTestUtils.createName() + "\n"
                + TranscriptomeManifest.Fields.PROGRAM      + " assembly\n"
                + TranscriptomeManifest.Fields.PLATFORM     + " fghgf\n"
                + TranscriptomeManifest.Fields.SAMPLE       + " SAMN04526268\n"
                + TranscriptomeManifest.Fields.STUDY        + " PRJEB20083\n";
    }

    private String
    getSequenceManifestFields()
    {
        return    SequenceManifest.Fields.NAME  + " " + WebinCliTestUtils.createName() + "\n"
                + SequenceManifest.Fields.STUDY + " PRJEB20083\n";
    }


    private void
    testWebinCli(ContextE context, Path inputDir, String manifestContents) throws Exception {
        WebinCli.Params parameters = new WebinCli.Params();
        parameters.context = context.toString();
        parameters.inputDir = inputDir.toString();
        parameters.outputDir  = WebinCliTestUtils.createTempDir().getPath();
        parameters.manifest = WebinCliTestUtils.createTempFile("manifest.txt", inputDir, false,
                manifestContents).toAbsolutePath().toString();
        parameters.userName = System.getenv( "webin-cli-username" );
        parameters.password = System.getenv( "webin-cli-password" );
        // parameters.centerName = "C E N T E R N A M E";
        parameters.test = true;
        parameters.validate = true;
        parameters.submit   = true;

        WebinCli webinCli = new WebinCli();
        webinCli.init( parameters );
        webinCli.setTestMode( true );
        webinCli.execute();
    }
    
    @Test public void
    testRawReadsSubmissionWithInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        Path cram_file = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/rawreads/18045_1#93.cram", input_dir, false );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", input_dir, false, getRawReadsInfoFields());

        testWebinCli(ContextE.reads, input_dir,
                ManifestReader.Fields.INFO + " " + infofile.getFileName() + "\n" +
                RawReadsManifest.Fields.CRAM + " " + cram_file.getFileName());
    }
    
    
    @Test public void
    testRawReadsSubmissionWithoutInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        Path cram_file = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/rawreads/18045_1#93.cram", input_dir, false );

        testWebinCli(ContextE.reads, input_dir,
                RawReadsManifest.Fields.CRAM + " " + cram_file.getFileName() + "\n" +
                getRawReadsInfoFields());
    }


    @Test public void
    testGenomeSubmissionWithInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        
        Path flatfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/assembly/valid_flatfileforAgp.txt", input_dir, true, ".gz" );
        Path agpfile  = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/assembly/valid_flatfileagp.txt", input_dir, true, ".agp.gz" );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", input_dir, false, getGenomeManifestInfoFields());

        testWebinCli(ContextE.genome, input_dir,
                ManifestReader.Fields.INFO + " " + infofile.getFileName() + "\n" +
                GenomeManifest.Fields.FLATFILE + " " + flatfile.getFileName() + "\n" +
                GenomeManifest.Fields.AGP + " " + agpfile.getFileName());
    }
    
    
    @Test public void
    testGenomeSubmissionWithoutInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        
        Path flatfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/assembly/valid_flatfileforAgp.txt", input_dir, true, ".gz" );
        Path agpfile  = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/assembly/valid_flatfileagp.txt", input_dir, true, ".agp.gz" );

        testWebinCli(ContextE.genome, input_dir,
                GenomeManifest.Fields.FLATFILE + " " + flatfile.getFileName() + "\n" +
                GenomeManifest.Fields.AGP + " " + agpfile.getFileName()       + "\n" +
                getGenomeManifestInfoFields());
    }
    
    
    
    @Test public void
    testSequenceSubmissionWithInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        
        Path tabfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/template/tsvfile/ERT000003-EST.tsv.gz", input_dir, false );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", input_dir, false, getSequenceManifestFields());

        testWebinCli(ContextE.sequence, input_dir,
                ManifestReader.Fields.INFO + " " + infofile.getFileName() + "\n" +
                SequenceManifest.Fields.TAB + " " + tabfile.getFileName());
    }
    
    
    @Test public void
    testSequenceSubmissionWithoutInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();
        
        Path tabfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/template/tsvfile/ERT000003-EST.tsv.gz", input_dir, false );

        testWebinCli(ContextE.sequence, input_dir,
                SequenceManifest.Fields.TAB + " " + tabfile.getFileName() + "\n" +
                getSequenceManifestFields());
    }


    //TODO update when inputDir supported
    @Test public void
    testTranscriptomeSubmissionWithInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();

        Path fastafile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/transcriptome/simple_fasta/transcriptome.fasta.gz", input_dir, false );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", input_dir, false, getTranscriptomeManifestFields());

        testWebinCli(ContextE.transcriptome, input_dir,
                ManifestReader.Fields.INFO + " " + infofile.getFileName() + "\n" +
                TranscriptomeManifest.Fields.FASTA + " " + fastafile.getFileName());
    }


    //TODO update when inputDir supported
    @Test public void
    testTranscriptomeSubmissionWithoutInfo() throws Exception
    {
        Path input_dir = WebinCliTestUtils.createTempDir().toPath();

        Path fastafile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/transcriptome/simple_fasta/transcriptome.fasta.gz", input_dir, false );

        testWebinCli(ContextE.transcriptome, input_dir,
                TranscriptomeManifest.Fields.FASTA + " " + fastafile.getFileName() + "\n" +
                getTranscriptomeManifestFields());
    }
}
