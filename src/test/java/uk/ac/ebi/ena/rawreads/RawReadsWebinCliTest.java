package uk.ac.ebi.ena.rawreads;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.assembly.GenomeAssemblyWebinCliTest;
import uk.ac.ebi.ena.rawreads.RawReadsManifest.RawReadsManifestTags;
import uk.ac.ebi.ena.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

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
    parseManifest() throws IOException, ValidationEngineException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        Path fastq_file = Files.write( File.createTempFile( "FASTQ", "FASTQ" ).toPath(), "@1.1\nACGT\n@\n!@#$\n".getBytes( StandardCharsets.UTF_8 ), StandardOpenOption.TRUNCATE_EXISTING );

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setCenterName( "C E N T E R N A M E" );
        parameters.setInputDir( fastq_file.getParent().toFile() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart() + "FASTQ PHRED_33 " + fastq_file.toString() ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }
    
    
    @Test( expected = WebinCliException.class ) public void
    manifestTwoBAMs() throws IOException, ValidationEngineException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setCenterName( "C E N T E R N A M E" );
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nBAM file1.bam\nBAM file2.bam" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }
    
    
    @Test( expected = WebinCliException.class ) public void
    manifestTwoCRAMs() throws IOException, ValidationEngineException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setCenterName( "C E N T E R N A M E" );
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nCRAM file1.cram\nCRAM file2.cram" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }

    
    @Test( expected = WebinCliException.class ) public void
    manifestMixingFormats() throws IOException, ValidationEngineException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setCenterName( "C E N T E R N A M E" );
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nBAM file1.bam\nCRAM file2.cram" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }
    
    
    @Test( expected = WebinCliException.class ) public void
    manifestNoFiles() throws IOException, ValidationEngineException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setCenterName( "C E N T E R N A M E" );
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }
    
    
    @Test( expected = WebinCliException.class ) public void
    manifestDoesFileNotExist() throws IOException, ValidationEngineException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setCenterName( "C E N T E R N A M E" );
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nFASTQ yoba.fastq.gz.bz2 PHRED_33" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }

    
    @Test( expected = WebinCliException.class ) public void
    manifestFileIsDirectory() throws IOException, ValidationEngineException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setCenterName( "C E N T E R N A M E" );
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nFASTQ " + createOutputFolder() + " PHRED_33" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }

    
    @Test( expected = WebinCliException.class ) public void
    manifestNoPath() throws IOException, ValidationEngineException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setCenterName( "C E N T E R N A M E" );
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nFASTQ" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }

    
    @Test( expected = WebinCliException.class ) public void
    manifestFastqNoScoring() throws IOException, ValidationEngineException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setCenterName( "C E N T E R N A M E" );
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nFASTQ file.fq.gz" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }
    
    
    @Test( expected = WebinCliException.class ) public void
    manifestBAMScoring() throws IOException, ValidationEngineException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setCenterName( "C E N T E R N A M E" );
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
    manifestBAMCompression() throws IOException, ValidationEngineException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setCenterName( "C E N T E R N A M E" );
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nBAM GZIP file.fq.gz" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }
   
    
    @Test( expected = WebinCliException.class ) public void
    manifestCRAMScoring() throws IOException, ValidationEngineException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setCenterName( "C E N T E R N A M E" );
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nCRAM PHRED_64 file.fq.gz" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }

    
    @Test( expected = WebinCliException.class ) public void
    manifestCRAMCompression() throws IOException, ValidationEngineException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();

        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setCenterName( "C E N T E R N A M E" );
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( "STUDY SRP123456789\nSAMPLE ERS198522\nPLATFORM ILLUMINA\nNAME SOME-FANCY-NAME\nCRAM GZIP file.fq.gz" ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFileList() );
    }


    @Test public void
    testCorrectBAM() throws IOException, ValidationEngineException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/rawreads/OUTO500m_MetOH_narG_OTU18.bam" );
        Path file = Paths.get( new File( url.getFile() ).getCanonicalPath() );
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setCenterName( "C E N T E R N A M E" );
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart() + "BAM " + file ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        Assert.assertTrue( "Should validate correctly", rr.validate() );
    }

    
    private String
    getInfoPart()
    {
        return    RawReadsManifestTags.STUDY             + " SRP123456789\n"
                + RawReadsManifestTags.SAMPLE            + " ERS198522\n"
                + RawReadsManifestTags.PLATFORM          + " ILLUMINA\n"
                + RawReadsManifestTags.INSTRUMENT        + " unspecifieD\n"
                + RawReadsManifestTags.INSERT_SIZE       + " 1\n"
                + RawReadsManifestTags.LIBRARY_STRATEGY  + " CLONEEND\n"
                + RawReadsManifestTags.LIBRARY_SOURCE    + " OTHER\n"
                + RawReadsManifestTags.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                + RawReadsManifestTags.NAME              + " SOME-FANCY-NAME\n ";
    }
    
    
    @Test public void
    testIncorrectBAM() throws IOException, ValidationEngineException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/rawreads/m54097_170904_165950.subreads.bam" );
        File file = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) );
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setCenterName( "C E N T E R N A M E" );
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart()
                                                 + "BAM " + file ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        
        Assert.assertTrue( "Should validate incorrectly", !rr.validate() );
        Assert.assertTrue( "Result file should exist", 1 == rr.getValidationDir().list( new FilenameFilter() { 
            @Override public boolean accept( File dir, String name ) 
            { 
                return name.contains( file.getName() ); 
            } } ).length );
    }

    
    @Test public void
    testCorrectFastq() throws IOException, ValidationEngineException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/rawreads/ZeroCycle_ES0_TTCCTT20NGA_0.txt.gz" );
        Path file = Paths.get( new File( url.getFile() ).getCanonicalPath() );
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setCenterName( "C E N T E R N A M E" );
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart() + "FASTQ GZ PHRED_33 " + file ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        Assert.assertTrue( "Should validate correctly", rr.validate() );
    }
    
    
    @Test public void
    testIncorrectFastq() throws IOException, ValidationEngineException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/rawreads/MG23S_431.fastq.gz" );
        File file = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) );
        
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setCenterName( "C E N T E R N A M E" );
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart() + "FASTQ GZ PHRED_33 " + file.getPath() ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        Assert.assertTrue( "Should validate incorrectly", !rr.validate() );
        Assert.assertTrue( "Result file should exist", 1 == rr.getValidationDir().list( new FilenameFilter() { 
            @Override public boolean accept( File dir, String name ) 
            { 
                return name.contains( file.getName() ); 
            } } ).length );
    }

    
    @Test public void
    testIncorrectCram() throws IOException, ValidationEngineException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/rawreads/15194_1#135.cram" );
        File file = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) );
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setCenterName( "C E N T E R N A M E" );
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart() + "CRAM " + file.getPath() ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        Assert.assertTrue( "Should validate incorrectly", !rr.validate() );
        Assert.assertTrue( "Result file should exist", 1 == rr.getValidationDir().list( new FilenameFilter() { 
            @Override public boolean accept( File dir, String name ) 
            { 
                return name.contains( file.getName() ); 
            } } ).length );
    }
    
  
    @Test public void
    testCorrectCram() throws IOException, ValidationEngineException
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/rawreads/18045_1#93.cram" );
        File file = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) );
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setCenterName( "C E N T E R N A M E" );
        parameters.setInputDir( createOutputFolder() );
        parameters.setManifestFile( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                                 ( getInfoPart() + "CRAM " + file.getPath() ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        Assert.assertTrue( "Should validate correctly", rr.validate() );
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
