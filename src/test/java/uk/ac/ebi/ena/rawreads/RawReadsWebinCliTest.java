package uk.ac.ebi.ena.rawreads;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.ena.rawreads.RawReadsFile.Filetype;
import uk.ac.ebi.ena.rawreads.RawReadsFile.QualityScoringSystem;
import uk.ac.ebi.ena.webin.cli.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

public class 
RawReadsWebinCliTest
{

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
    parseFileLineTest()
    {
        RawReadsWebinCli rr = new RawReadsWebinCli();
        
        RawReadsFile file = rr.parseFileLine( new String[] { "FASTQ", "PHRED_33", "fastq.file", "" } );
        Assert.assertTrue( file.getFilename().contains( "fastq.file" ) );
        Assert.assertEquals( Filetype.fastq, file.getFiletype() );
        Assert.assertEquals( QualityScoringSystem.phred, file.getQualityScoringSystem());

        
        file = rr.parseFileLine( new String[] { "", "fastq.file", "FASTQ", "PHRED_33", "" } );
        Assert.assertTrue( file.getFilename().contains( "fastq.file" ) );
        Assert.assertEquals( Filetype.fastq, file.getFiletype() );
        Assert.assertEquals( QualityScoringSystem.phred, file.getQualityScoringSystem());

        file = rr.parseFileLine( new String[] { "", "fastq.file", "BAM", "LOGODDS", "" } );
        Assert.assertTrue( file.getFilename().contains( "fastq.file" ) );
        Assert.assertEquals( Filetype.bam, file.getFiletype() );
        Assert.assertEquals( QualityScoringSystem.log_odds, file.getQualityScoringSystem());

        System.out.println( file );
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
                                                 ( "EXPERIMENT-ID ERX123456789\nFASTQ PHRED_33 " + fastq_file.toString() ).getBytes( StandardCharsets.UTF_8 ), 
                                                 StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        parameters.setOutputDir( createOutputFolder() );
        
        rr.init( parameters );
        rr.prepareSubmissionBundle();
        SubmissionBundle sb = rr.getSubmissionBundle();
        System.out.println( sb.getXMLFile() );
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
