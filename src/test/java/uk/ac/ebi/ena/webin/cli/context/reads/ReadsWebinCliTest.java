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
package uk.ac.ebi.ena.webin.cli.context.reads;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;

import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.readtools.cram.ref.ENAReferenceSource;
import uk.ac.ebi.ena.readtools.cram.ref.ENAReferenceSource.LoggerWrapper;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle.SubmissionXMLFileType;

import static uk.ac.ebi.ena.webin.cli.context.reads.ReadsManifestReader.Field;

public class
ReadsWebinCliTest
{
    @Before public void
    before()
    {
        ValidationMessage.setDefaultMessageFormatter( ValidationMessage.TEXT_TIME_MESSAGE_FORMATTER_TRAILING_LINE_END );
        ValidationResult.setDefaultMessageFormatter( null );
    }


    @Test public void
    parseManifest() throws IOException {
        Path fastq_file = WebinCliTestUtils.createGzippedTempFile("fastq.gz", "@1.1\nACGT\n@\n!@#$\n");

        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( fastq_file.getParent().toFile() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( getInfoPart() + "FASTQ " + fastq_file.toString() ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setMetadataProcessorsActive(false);
        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }


    @Test( expected = WebinCliException.class ) public void
    manifestTwoBAMs() throws IOException {
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nBAM file1.bam\nBAM file2.bam" ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setMetadataProcessorsActive(false);
        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }


    @Test( expected = WebinCliException.class ) public void
    manifestTwoCRAMs() throws IOException {
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nCRAM file1.cram\nCRAM file2.cram" ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setMetadataProcessorsActive(false);

        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }


    @Test( expected = WebinCliException.class ) public void
    manifestMixingFormats() throws IOException {
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nBAM file1.bam\nCRAM file2.cram" ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );

        parameters.setMetadataProcessorsActive(false);

        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }


    @Test( expected = WebinCliException.class ) public void
    manifestNoFiles() throws IOException {
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME" ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );

        parameters.setMetadataProcessorsActive(false);

        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }


    @Test( expected = WebinCliException.class ) public void
    manifestDoesFileNotExist() throws IOException {
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nFASTQ yoba.fastq.gz.bz2 PHRED_33" ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );

        parameters.setMetadataProcessorsActive(false);
        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }


    @Test( expected = WebinCliException.class ) public void
    manifestFileIsDirectory() throws IOException {
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nFASTQ " + createOutputFolder() + " PHRED_33" ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );

        parameters.setMetadataProcessorsActive(false);
        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }


    @Test( expected = WebinCliException.class ) public void
    manifestNoPath() throws IOException {
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nFASTQ" ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );

        parameters.setMetadataProcessorsActive(false);
        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }


    @Test( expected = WebinCliException.class ) public void
    manifestNonASCIIPath() throws IOException {
        URL url = ReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/MG23S_431.fastq.gz" );
        File gz = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) );

        Path file = Files.write( Files.createTempFile( "FILE", "Š.fq.gz" ), Files.readAllBytes( gz.toPath() ), StandardOpenOption.TRUNCATE_EXISTING );
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( getInfoPart() + "FASTQ " + file ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );

        parameters.setMetadataProcessorsActive(false);
        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }


    @Test( expected = WebinCliException.class ) public void
    manifestFastqNoScoring() throws IOException {
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nFASTQ file.fq.gz" ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );

        parameters.setMetadataProcessorsActive(false);
        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }


    @Test( expected = WebinCliException.class ) public void
    manifestBAMScoring() throws IOException {
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nBAM PHRED_33 file.fq.gz" ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );


        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }


    @Test( expected = WebinCliException.class ) public void
    manifestBAMCompression() throws IOException {
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nBAM file.fq.gz" ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );

        parameters.setMetadataProcessorsActive(false);

        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }


    @Test( expected = WebinCliException.class ) public void
    manifestCRAMScoring() throws IOException {
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nCRAM file.fq.gz" ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );


        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }


    @Test( expected = WebinCliException.class ) public void
    manifestCRAMCompression() throws IOException
    {
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nCRAM file.fq.gz" ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );

        parameters.setMetadataProcessorsActive(false);

        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }

    @Test public void
    testCorrectBAM() throws IOException
    {
        URL url = ReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/OUTO500m_MetOH_narG_OTU18.bam" );
        Path file = Paths.get( new File( url.getFile() ).getCanonicalPath() );
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( getInfoPart() + "BAM " + file ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );

        parameters.setMetadataProcessorsActive(false);
        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.validateSubmission();
    }


    private String
    getInfoPart()
    {
        return    Field.STUDY             + " ERP109454\n"
                + Field.SAMPLE            + " ERS2713291\n"
                + Field.PLATFORM          + " ILLUMINA\n"
                + Field.INSTRUMENT        + " unspecifieD\n"
                + Field.INSERT_SIZE       + " 1\n"
                + Field.LIBRARY_STRATEGY  + " CLONEEND\n"
                + Field.LIBRARY_SOURCE    + " OTHER\n"
                + Field.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                + Field.NAME              + " SOME-FANCY-NAME\n ";
    }


    @Test public void
    testIncorrectBAM() throws IOException
    {
        URL url = ReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/m54097_170904_165950.subreads.bam" );
        File file = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) );
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( getInfoPart()
                                                 + "BAM " + file ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );


        parameters.setMetadataProcessorsActive(false);

        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();

        try
        {
            executor.validateSubmission();
            fail( "Should not validate correctly" );

        } catch( WebinCliException wce )
        {
            Assert.assertEquals("Result file should exist", 1, executor.getValidationDir().list(
                    (dir, name) -> name.contains(file.getName())).length);
        }
    }


    @Test public void
    testCorrectFastq() throws IOException
    {
        URL url = ReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/ZeroCycle_ES0_TTCCTT20NGA_0.txt.gz" );
        Path file = Paths.get( new File( url.getFile() ).getCanonicalPath() );
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( WebinCliTestUtils.createEmptyTempFile(),
                                                 ( getInfoPart() + "FASTQ " + file ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );

        parameters.setMetadataProcessorsActive(false);
        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.validateSubmission();
        executor.prepareSubmissionBundle();
        String lines = new String( Files.readAllBytes( executor.readSubmissionBundle().getXMLFileList().stream().filter(e->SubmissionXMLFileType.EXPERIMENT.equals( e.getType() ) ).findFirst().get().getFile().toPath() ),
                                   StandardCharsets.UTF_8 );
        Assert.assertTrue( lines.contains( "<SINGLE />" ) );
    }


    @Test( expected = WebinCliException.class ) public void
    sameFilePairedFastq() throws IOException
    {
        URL url = ReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/ZeroCycle_ES0_TTCCTT20NGA_0.txt.gz" );
        Path file = Paths.get( new File( url.getFile() ).getCanonicalPath() );
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( getInfoPart()
                                                 + "FASTQ " + file + "\n"
                                                 + "FASTQ " + file ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );

        parameters.setMetadataProcessorsActive(false);
        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.validateSubmission();
    }


    @Test( expected = WebinCliException.class ) public void
    samePairedFastq() throws IOException
    {
        URL url = ReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/EP0_GTTCCTT_0.txt.gz" );
        Path file = Paths.get( new File( url.getFile() ).getCanonicalPath() );
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( getInfoPart()
                                                 + "FASTQ " + file + "\n"
                                                 + "FASTQ " + file ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );

        parameters.setMetadataProcessorsActive(false);
        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.validateSubmission();
    }


    @Test public void
    pairedFastq() throws IOException
    {
        URL url = ReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/EP0_GTTCCTT_0.txt.gz" );
        Path file = Paths.get( new File( url.getFile() ).getCanonicalPath() );
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( getInfoPart()
                                                 + "FASTQ " + file ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );

        parameters.setMetadataProcessorsActive(false);
        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.validateSubmission();
        executor.prepareSubmissionBundle();
        String lines = new String( Files.readAllBytes( executor.readSubmissionBundle().getXMLFileList().stream().filter(e->SubmissionXMLFileType.EXPERIMENT.equals( e.getType() ) ).findFirst().get().getFile().toPath() ),
                                   StandardCharsets.UTF_8 );
        Assert.assertTrue( lines.contains( "<PAIRED" ) );
    }


    @Test public void
    fastqPair() throws IOException
    {
        URL  url1 = ReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/EP0_GTTCCTT_S1.txt.gz" );
        Path file1 = Paths.get( new File( url1.getFile() ).getCanonicalPath() );
        URL  url2 = ReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/EP0_GTTCCTT_S2.txt.gz" );
        Path file2 = Paths.get( new File( url2.getFile() ).getCanonicalPath() );

        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( getInfoPart()
                                                 + "FASTQ " + file1 + "\n"
                                                 + "FASTQ " + file2 ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );

        parameters.setMetadataProcessorsActive(false);
        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.validateSubmission();
        executor.prepareSubmissionBundle();
        String rlines = new String( Files.readAllBytes( executor.readSubmissionBundle().getXMLFileList().stream().filter(e->SubmissionXMLFileType.RUN.equals( e.getType() ) ).findFirst().get().getFile().toPath() ),
                                    StandardCharsets.UTF_8 );
        Assert.assertTrue( rlines.contains( file1.getFileName().toString() ) );
        Assert.assertTrue( rlines.contains( file2.getFileName().toString() ) );

        String elines = new String( Files.readAllBytes( executor.readSubmissionBundle().getXMLFileList().stream().filter(e->SubmissionXMLFileType.EXPERIMENT.equals( e.getType() ) ).findFirst().get().getFile().toPath() ),
                                   StandardCharsets.UTF_8 );
        Assert.assertTrue( elines.contains( "<PAIRED" ) );
    }

    //TODO remove?
    @Test( expected = WebinCliException.class ) public void
    fastqFalsePair() throws IOException
    {
        URL  url1 = ReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/EP0_GTTCCTT_P1.txt.gz" );
        Path file1 = Paths.get( new File( url1.getFile() ).getCanonicalPath() );
        URL  url2 = ReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/EP0_GTTCCTT_P2.txt.gz" );
        Path file2 = Paths.get( new File( url2.getFile() ).getCanonicalPath() );

        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( getInfoPart()
                                                 + "FASTQ " + file1 + "\n"
                                                 + "FASTQ " + file2 ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );

        parameters.setMetadataProcessorsActive(false);
        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.validateSubmission();
    }


    @Test public void
    testIncorrectFastq() throws IOException
    {
        URL url = ReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/MG23S_431.fastq.gz" );
        File file = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) );

        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( getInfoPart() + "FASTQ " + file.getPath() ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );

        parameters.setMetadataProcessorsActive(false);
        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        try
        {
            executor.validateSubmission();
            fail( "Should validate incorrectly" );

        } catch( WebinCliException wce )
        {
            Assert.assertEquals("Result file should exist", 1, executor.getValidationDir().list(
                    (dir, name) -> name.contains(file.getName())).length);
        }
    }


    @Test public void
    testIncorrectCram() throws IOException
    {
        URL url = ReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/15194_1#135.cram" );
        File file = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) );
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( getInfoPart() + "CRAM " + file.getPath() ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );

        parameters.setMetadataProcessorsActive(false);
        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        try
        {
            executor.validateSubmission();
            fail( "Should validate incorrectly" );

        } catch( WebinCliException wce )
        {
            Assert.assertEquals("Result file should exist", 1, executor.getValidationDir().list(
                    (dir, name) -> name.contains(file.getName())).length);
        }
    }


    @Test public void
    testCorrectCram() throws IOException
    {
        URL url = ReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/18045_1#93.cram" );
        File file = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) );
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(),
                                                 ( getInfoPart() + "CRAM " + file.getPath() ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );

        parameters.setMetadataProcessorsActive(false);
        ReadsWebinCliExecutor executor = new ReadsWebinCliExecutor( parameters );
        executor.readManifest();
        executor.validateSubmission();
    }



    @Ignore @Test( timeout = 200_000 ) public void
    openSamExamples() throws UnsupportedEncodingException
    {
        final SamReaderFactory factory =
                SamReaderFactory.makeDefault().enable( SamReaderFactory.Option.VALIDATE_CRC_CHECKSUMS ).validationStringency( ValidationStringency.LENIENT );

        URL url = ReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/20416_1#274.cram" );
        final SamInputResource resource = SamInputResource.of( new File( URLDecoder.decode( url.getFile(), "UTF-8" ) ) );
        ENAReferenceSource rs = new ENAReferenceSource( new String[] { } );
        rs.setLoggerWrapper( new LoggerWrapper() {

            @Override public void
            debug( Object... messageParts )
            {
                System.out.println( Arrays.asList( messageParts ) );
            }

            @Override public void
            warn( Object... messageParts )
            {
                System.out.println( Arrays.asList( messageParts ) );
            }

            @Override public void
            error( Object... messageParts )
            {
                System.out.println( Arrays.asList( messageParts ) );
            }

            @Override public void
            info( Object... messageParts )
            {
                System.out.println( Arrays.asList( messageParts ) );
            }

        } );
        factory.referenceSource( rs );
        final SamReader myReader = factory.open(resource);

        for (final SAMRecord samRecord : myReader)
        {
            System.err.print(samRecord);
        }

    }


    @Test( timeout = 200_000 ) public void
    openSamSmall() throws UnsupportedEncodingException
    {
        final SamReaderFactory factory =
                SamReaderFactory.makeDefault().enable( SamReaderFactory.Option.VALIDATE_CRC_CHECKSUMS ).validationStringency( ValidationStringency.LENIENT );

        URL url = ReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/16258_6#32.cram" );
        final SamInputResource resource = SamInputResource.of( new File( URLDecoder.decode( url.getFile(), "UTF-8" ) ) );
        ENAReferenceSource rs = new ENAReferenceSource( new String[] { } );
        rs.setLoggerWrapper( new LoggerWrapper() {

            @Override public void
            debug( Object... messageParts )
            {
                System.out.println( Arrays.asList( messageParts ) );
            }

            @Override public void
            warn( Object... messageParts )
            {
                System.out.println( Arrays.asList( messageParts ) );
            }

            @Override public void
            error( Object... messageParts )
            {
                System.out.println( Arrays.asList( messageParts ) );
            }

            @Override public void
            info( Object... messageParts )
            {
                System.out.println( Arrays.asList( messageParts ) );
            }

        } );
        factory.referenceSource( rs );
        final SamReader myReader = factory.open(resource);

        for (final SAMRecord samRecord : myReader)
        {
            System.err.print(samRecord);
        }

    }


    private File
    createOutputFolder() throws IOException
    {
        File output = File.createTempFile( "test", "test" );
        Assert.assertTrue( output.delete() );
        Assert.assertTrue( output.mkdirs() );
        return output;
    }
}
