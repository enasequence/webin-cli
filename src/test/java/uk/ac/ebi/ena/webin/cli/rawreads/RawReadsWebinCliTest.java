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
package uk.ac.ebi.ena.webin.cli.rawreads;

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
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import net.sf.cram.ref.ENAReferenceSource;
import net.sf.cram.ref.ENAReferenceSource.LoggerWrapper;

import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsManifest.Fields;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle.SubmissionXMLFileType;

public class 
RawReadsWebinCliTest
{
    @Before public void
    before()
    {
        ValidationMessage.setDefaultMessageFormatter( ValidationMessage.TEXT_TIME_MESSAGE_FORMATTER_TRAILING_LINE_END );
        ValidationResult.setDefaultMessageFormatter( null );
    }
    
    

    @Test public void
    testXML()
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        rr.setName( "SOME-NAME" );
        String result = rr.createRunXml( Collections.emptyList(), "SOME-EXPERIMENT-ID", "CENTER-NAME" );
        
        Assert.assertTrue( result.contains( "SOME-NAME" ) );
        Assert.assertTrue( result.contains( "SOME-EXPERIMENT-ID" ) );
        Assert.assertTrue( result.contains( "CENTER-NAME" ) );
    }
    
 
    @Test public void
    parseManifest() throws IOException {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        Path fastq_file = WebinCliTestUtils.createGzippedTempFile("fastq.gz", "@1.1\nACGT\n@\n!@#$\n");

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( fastq_file.getParent().toFile() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart() + "FASTQ " + fastq_file.toString() ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }
    
    
    @Test( expected = WebinCliException.class ) public void
    manifestTwoBAMs() throws IOException {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nBAM file1.bam\nBAM file2.bam" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }
    
    
    @Test( expected = WebinCliException.class ) public void
    manifestTwoCRAMs() throws IOException {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nCRAM file1.cram\nCRAM file2.cram" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }

    
    @Test( expected = WebinCliException.class ) public void
    manifestMixingFormats() throws IOException {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nBAM file1.bam\nCRAM file2.cram" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }
    
    
    @Test( expected = WebinCliException.class ) public void
    manifestNoFiles() throws IOException {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }
    
    
    @Test( expected = WebinCliException.class ) public void
    manifestDoesFileNotExist() throws IOException {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nFASTQ yoba.fastq.gz.bz2 PHRED_33" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }

    
    @Test( expected = WebinCliException.class ) public void
    manifestFileIsDirectory() throws IOException {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nFASTQ " + createOutputFolder() + " PHRED_33" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }

    
    @Test( expected = WebinCliException.class ) public void
    manifestNoPath() throws IOException {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nFASTQ" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }

    
    @Test( expected = WebinCliException.class ) public void
    manifestNonASCIIPath() throws IOException {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        URL url = RawReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/MG23S_431.fastq.gz" );
        File gz = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) );

        Path file = Files.write( Files.createTempFile( "FILE", "Š.fq.gz" ), Files.readAllBytes( gz.toPath() ), StandardOpenOption.TRUNCATE_EXISTING );
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart() + "FASTQ " + file ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }

    
    @Test( expected = WebinCliException.class ) public void
    manifestFastqNoScoring() throws IOException {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nFASTQ file.fq.gz" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }
    
    
    @Test( expected = WebinCliException.class ) public void
    manifestBAMScoring() throws IOException {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nBAM PHRED_33 file.fq.gz" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }

    
    @Test( expected = WebinCliException.class ) public void
    manifestBAMCompression() throws IOException {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nBAM file.fq.gz" ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }
   
    
    @Test( expected = WebinCliException.class ) public void
    manifestCRAMScoring() throws IOException {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nCRAM file.fq.gz" ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }

    
    @Test( expected = WebinCliException.class ) public void
    manifestCRAMCompression() throws IOException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nCRAM file.fq.gz" ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }


    @Test public void
    testCorrectBAMManifest() throws IOException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        URL url = RawReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/OUTO500m_MetOH_narG_OTU18.bam" );
        Path file = Paths.get( new File( url.getFile() ).getCanonicalPath() );
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart() + "BAM " + file + "\n" 
                                                 + Fields.DESCRIPTION + " description text" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
        WebinCliTestUtils.assertAnalysisXml( new String( Files.readAllBytes( sb.getXMLFileList().get( 0 ).getFile().toPath() ), StandardCharsets.UTF_8 ), 
       "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<EXPERIMENT_SET>\n"
      + " <EXPERIMENT alias=\"webin-reads-SOME-FANCY-NAME\">\n"
      + "  <TITLE>Raw reads: SOME-FANCY-NAME</TITLE>\n"
      + "   <STUDY_REF accession=\"ERP109454\" />\n"
      + "    <DESIGN>\n"
      + "     <DESIGN_DESCRIPTION>description text</DESIGN_DESCRIPTION>\n"
      + "     <SAMPLE_DESCRIPTOR accession=\"ERS2713291\" />\n"
      + "     <LIBRARY_DESCRIPTOR>\n"
      + "       <LIBRARY_STRATEGY>CLONEEND</LIBRARY_STRATEGY>\n"
      + "       <LIBRARY_SOURCE>OTHER</LIBRARY_SOURCE>\n"
      + "       <LIBRARY_SELECTION>Inverse rRNA selection</LIBRARY_SELECTION>\n"
      + "       <LIBRARY_LAYOUT>\n"
      + "        <SINGLE />\n"
      + "       </LIBRARY_LAYOUT>\n"
      + "     </LIBRARY_DESCRIPTOR>\n"
      + "    </DESIGN>\n"
      + "    <PLATFORM>\n"
      + "     <ILLUMINA>\n"
      + "       <INSTRUMENT_MODEL>unspecified</INSTRUMENT_MODEL>\n"
      + "     </ILLUMINA>\n"
      + "    </PLATFORM>\n"
      + " </EXPERIMENT>\n"
      + "</EXPERIMENT_SET>" );
    }

    
    @Test public void
    testCorrectBAM() throws IOException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        URL url = RawReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/OUTO500m_MetOH_narG_OTU18.bam" );
        Path file = Paths.get( new File( url.getFile() ).getCanonicalPath() );
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart() + "BAM " + file ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        rr.init( parameters );
        rr.validate();
    }

    
    private String
    getInfoPart()
    {
        return    Fields.STUDY             + " ERP109454\n"
                + Fields.SAMPLE            + " ERS2713291\n"
                + Fields.PLATFORM          + " ILLUMINA\n"
                + Fields.INSTRUMENT        + " unspecifieD\n"
                + Fields.INSERT_SIZE       + " 1\n"
                + Fields.LIBRARY_STRATEGY  + " CLONEEND\n"
                + Fields.LIBRARY_SOURCE    + " OTHER\n"
                + Fields.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                + Fields.NAME              + " SOME-FANCY-NAME\n ";
    }
    
    
    @Test public void
    testIncorrectBAM() throws IOException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        URL url = RawReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/m54097_170904_165950.subreads.bam" );
        File file = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) );
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart()
                                                 + "BAM " + file ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        
        rr.init( parameters );
        
        try
        {
            rr.validate();
            fail( "Should not validate correctly" );
            
        } catch( WebinCliException wce )
        {
            Assert.assertEquals("Result file should exist", 1, rr.getValidationDir().list(
                    (dir, name) -> name.contains(file.getName())).length);
        }
    }

    
    @Test public void
    testCorrectFastq() throws IOException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        URL url = RawReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/ZeroCycle_ES0_TTCCTT20NGA_0.txt.gz" );
        Path file = Paths.get( new File( url.getFile() ).getCanonicalPath() );
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( WebinCliTestUtils.createEmptyTempFile(),
                                                 ( getInfoPart() + "FASTQ " + file ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        rr.init( parameters );
        rr.validate();
        rr.prepareSubmissionBundle();
        String lines = new String( Files.readAllBytes( rr.getSubmissionBundle().getXMLFileList().stream().filter( e->SubmissionXMLFileType.EXPERIMENT.equals( e.getType() ) ).findFirst().get().getFile().toPath() ),
                                   StandardCharsets.UTF_8 );
        Assert.assertTrue( lines.contains( "<SINGLE />" ) );
    }
    
    
    @Test( expected = WebinCliException.class ) public void
    sameFilePairedFastq() throws IOException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        
        URL url = RawReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/ZeroCycle_ES0_TTCCTT20NGA_0.txt.gz" );
        Path file = Paths.get( new File( url.getFile() ).getCanonicalPath() );
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart() 
                                                 + "FASTQ " + file + "\n"
                                                 + "FASTQ " + file ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        rr.init( parameters );
        rr.validate();
    }
    
    
    @Test( expected = WebinCliException.class ) public void
    samePairedFastq() throws IOException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        
        URL url = RawReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/EP0_GTTCCTT_0.txt.gz" );
        Path file = Paths.get( new File( url.getFile() ).getCanonicalPath() );
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart() 
                                                 + "FASTQ " + file + "\n"
                                                 + "FASTQ " + file ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        rr.init( parameters );
        rr.validate();
    }
    
    
    @Test public void
    pairedFastq() throws IOException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        
        URL url = RawReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/EP0_GTTCCTT_0.txt.gz" );
        Path file = Paths.get( new File( url.getFile() ).getCanonicalPath() );
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart() 
                                                 + "FASTQ " + file ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        rr.init( parameters );
        rr.validate();
        rr.prepareSubmissionBundle();
        String lines = new String( Files.readAllBytes( rr.getSubmissionBundle().getXMLFileList().stream().filter( e->SubmissionXMLFileType.EXPERIMENT.equals( e.getType() ) ).findFirst().get().getFile().toPath() ),
                                   StandardCharsets.UTF_8 );
        Assert.assertTrue( lines.contains( "<PAIRED" ) );
    }
    
    
    @Test public void
    fastqPair() throws IOException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        
        URL  url1 = RawReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/EP0_GTTCCTT_S1.txt.gz" );
        Path file1 = Paths.get( new File( url1.getFile() ).getCanonicalPath() );
        URL  url2 = RawReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/EP0_GTTCCTT_S2.txt.gz" );
        Path file2 = Paths.get( new File( url2.getFile() ).getCanonicalPath() );

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart() 
                                                 + "FASTQ " + file1 + "\n" 
                                                 + "FASTQ " + file2 ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        rr.init( parameters );
        rr.validate();
        rr.prepareSubmissionBundle();
        String rlines = new String( Files.readAllBytes( rr.getSubmissionBundle().getXMLFileList().stream().filter( e->SubmissionXMLFileType.RUN.equals( e.getType() ) ).findFirst().get().getFile().toPath() ),
                                    StandardCharsets.UTF_8 );
        Assert.assertTrue( rlines.contains( file1.getFileName().toString() ) );
        Assert.assertTrue( rlines.contains( file2.getFileName().toString() ) );
        
        String elines = new String( Files.readAllBytes( rr.getSubmissionBundle().getXMLFileList().stream().filter( e->SubmissionXMLFileType.EXPERIMENT.equals( e.getType() ) ).findFirst().get().getFile().toPath() ),
                                   StandardCharsets.UTF_8 );
        Assert.assertTrue( elines.contains( "<PAIRED" ) );
    }
    
    //TODO remove?
    @Test( expected = WebinCliException.class ) public void
    fastqFalsePair() throws IOException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        
        URL  url1 = RawReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/EP0_GTTCCTT_P1.txt.gz" );
        Path file1 = Paths.get( new File( url1.getFile() ).getCanonicalPath() );
        URL  url2 = RawReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/EP0_GTTCCTT_P2.txt.gz" );
        Path file2 = Paths.get( new File( url2.getFile() ).getCanonicalPath() );

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart() 
                                                 + "FASTQ " + file1 + "\n" 
                                                 + "FASTQ " + file2 ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        rr.init( parameters );
        rr.validate();
    }

    
    @Test public void
    testIncorrectFastq() throws IOException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        URL url = RawReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/MG23S_431.fastq.gz" );
        File file = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) );
        
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart() + "FASTQ " + file.getPath() ).getBytes( StandardCharsets.UTF_8 ),
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        rr.init( parameters );
        try
        {
            rr.validate();
            fail( "Should validate incorrectly" );
            
        } catch( WebinCliException wce )
        {
            Assert.assertEquals("Result file should exist", 1, rr.getValidationDir().list(
                    (dir, name) -> name.contains(file.getName())).length);
        }
    }

    
    @Test public void
    testIncorrectCram() throws IOException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        URL url = RawReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/15194_1#135.cram" );
        File file = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) );
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart() + "CRAM " + file.getPath() ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        rr.init( parameters );
        try
        {
            rr.validate();
            fail( "Should validate incorrectly" );
            
        } catch( WebinCliException wce )
        {
            Assert.assertEquals("Result file should exist", 1, rr.getValidationDir().list(
                    (dir, name) -> name.contains(file.getName())).length);
        }
    }
    
  
    @Test public void
    testCorrectCram() throws IOException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        URL url = RawReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/18045_1#93.cram" );
        File file = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) );
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart() + "CRAM " + file.getPath() ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        rr.setFetchSample( false );
        rr.setFetchStudy( false );
        rr.init( parameters );
        rr.validate();
    }

    
    
    @Ignore @Test( timeout = 200_000 ) public void
    openSamExamples() throws UnsupportedEncodingException
    {
        final SamReaderFactory factory =
                SamReaderFactory.makeDefault().enable( SamReaderFactory.Option.VALIDATE_CRC_CHECKSUMS ).validationStringency( ValidationStringency.LENIENT );
        
        URL url = RawReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/20416_1#274.cram" );
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

        URL url = RawReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/16258_6#32.cram" );
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
