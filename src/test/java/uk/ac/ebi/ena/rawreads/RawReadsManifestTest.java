package uk.ac.ebi.ena.rawreads;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.rawreads.RawReadsFile.Filetype;
import uk.ac.ebi.ena.rawreads.RawReadsFile.QualityScoringSystem;
import uk.ac.ebi.ena.rawreads.RawReadsManifest.RawReadsManifestTags;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class 
RawReadsManifestTest
{
    @Before public void
    before()
    {
        ValidationMessage.setDefaultMessageFormatter( ValidationMessage.TEXT_TIME_MESSAGE_FORMATTER_TRAILING_LINE_END );
        ValidationResult.setDefaultMessageFormatter( null );
        Locale.setDefault( Locale.UK );
    }
    
    
    @Test public void
    parseFileLineTest()
    {
        RawReadsManifest rr = new RawReadsManifest();
        
        RawReadsFile file = rr.parseFileLine( Paths.get( "." ), new String[] { "FASTQ", "PHRED_33", "fastq.file", "" } );
        Assert.assertTrue( file.getFilename().contains( "fastq.file" ) );
        Assert.assertEquals( Filetype.fastq, file.getFiletype() );
        Assert.assertEquals( QualityScoringSystem.phred, file.getQualityScoringSystem());

        
        file = rr.parseFileLine( Paths.get( "." ), new String[] { "", "fastq.file", "FASTQ", "PHRED_33", "" } );
        Assert.assertTrue( file.getFilename().contains( "fastq.file" ) );
        Assert.assertEquals( Filetype.fastq, file.getFiletype() );
        Assert.assertEquals( QualityScoringSystem.phred, file.getQualityScoringSystem());

        file = rr.parseFileLine( Paths.get( "." ), new String[] { "", "fastq.file", "BAM", "LOGODDS", "" } );
        Assert.assertTrue( file.getFilename().contains( "fastq.file" ) );
        Assert.assertEquals( Filetype.bam, file.getFiletype() );
        Assert.assertEquals( QualityScoringSystem.log_odds, file.getQualityScoringSystem());

        System.out.println( file );
    }
    
    
    
    @Test public void
    testManifestFields() throws IOException
    {
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                                ( RawReadsManifestTags.STUDY             + " SRP123456789\n"
                                + RawReadsManifestTags.SAMPLE            + " ERS198522\n"
                                + RawReadsManifestTags.PLATFORM          + " illumina\n"
                                + RawReadsManifestTags.INSTRUMENT        + " Illumina HiScanSQ\n"
                                + RawReadsManifestTags.LIBRARY_STRATEGY  + " CLONEEND\n"
                                + RawReadsManifestTags.LIBRARY_SOURCE    + " OTHER\n"
                                + RawReadsManifestTags.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                                + RawReadsManifestTags.LIBRARY_NAME      + " Name library\n"
                                + RawReadsManifestTags.LIBRARY_CONSTRUCTION_PROTOCOL + " library construction protocol\n"
                                + RawReadsManifestTags.INSERT_SIZE       + " 100500\n"
                                + RawReadsManifestTags.NAME              + " SOME-FANCY-NAME\n "
                                + "BAM " + Files.createTempFile( "TEMP", "FILE" ) ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );
        RawReadsManifest rm = new RawReadsManifest();
        Assert.assertNull( rm.getStudyId() );
        Assert.assertNull( rm.getSampleId() );
        Assert.assertNull( rm.getPlatform() );
        Assert.assertNull( rm.getInstrument() );
        Assert.assertNull( rm.getLibraryStrategy() );
        Assert.assertNull( rm.getLibrarySource() );
        Assert.assertNull( rm.getLibrarySelection() );
        Assert.assertNull( rm.getLibraryName() );
        Assert.assertNull( rm.getLibraryConstructionProtocol() );
        Assert.assertNull( rm.getInsertSize() );
        Assert.assertNull( rm.getName() );
        Assert.assertNull( rm.getFiles() );
        
        rm.defineFileTypes( Paths.get( "." ), man.toFile() );

        Assert.assertEquals( "SRP123456789", rm.getStudyId() );
        Assert.assertEquals( "ERS198522", rm.getSampleId() );
        Assert.assertEquals( "ILLUMINA", rm.getPlatform() );
        Assert.assertEquals( "Illumina HiScanSQ", rm.getInstrument() );
        Assert.assertEquals( "CLONEEND", rm.getLibraryStrategy() );
        Assert.assertEquals( "OTHER", rm.getLibrarySource() );
        Assert.assertEquals( "Inverse rRNA selection", rm.getLibrarySelection() );
        Assert.assertEquals( "Name library", rm.getLibraryName() );
        Assert.assertEquals( "library construction protocol", rm.getLibraryConstructionProtocol() );
        Assert.assertEquals( new Integer( 100500 ), rm.getInsertSize() );
        Assert.assertEquals( "SOME-FANCY-NAME", rm.getName() );
        Assert.assertEquals( 1, rm.getFiles().size() );
    }


    @Test public void
    testManifestFieldsWithInfo() throws IOException
    {
        Path inf = Files.write( Files.createTempFile( Files.createTempDirectory( "TEMP" ), "TEMP", "INFO" ), 
                                ( RawReadsManifestTags.STUDY             + " SRP123456789\n"
                                + RawReadsManifestTags.SAMPLE            + " ERS198522\n"
                                + RawReadsManifestTags.PLATFORM          + " illumina\n"
                                + RawReadsManifestTags.INSTRUMENT        + " Illumina HiScanSQ\n"
                                + RawReadsManifestTags.LIBRARY_STRATEGY  + " CLONEEND\n"
                                + RawReadsManifestTags.LIBRARY_SOURCE    + " OTHER\n"
                                + RawReadsManifestTags.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                                + RawReadsManifestTags.LIBRARY_NAME      + " Name library\n"
                                + RawReadsManifestTags.LIBRARY_CONSTRUCTION_PROTOCOL + " library construction protocol\n"
                                + RawReadsManifestTags.INSERT_SIZE       + " 100500\n"
                                + RawReadsManifestTags.NAME              + " SOME-FANCY-NAME\n" ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ),
                                ( "INFO " + inf.getFileName() + "\n"
                                + "BAM " + Files.createTempFile( inf.getParent(), "TEMP", "FILE" ).getFileName() ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );
                                
        RawReadsManifest rm = new RawReadsManifest();
        
        Assert.assertNull( rm.getStudyId() );
        Assert.assertNull( rm.getSampleId() );
        Assert.assertNull( rm.getPlatform() );
        Assert.assertNull( rm.getInstrument() );
        Assert.assertNull( rm.getLibraryStrategy() );
        Assert.assertNull( rm.getLibrarySource() );
        Assert.assertNull( rm.getLibrarySelection() );
        Assert.assertNull( rm.getLibraryName() );
        Assert.assertNull( rm.getLibraryConstructionProtocol() );
        Assert.assertNull( rm.getInsertSize() );
        Assert.assertNull( rm.getName() );
        Assert.assertNull( rm.getFiles() );
        
        rm.defineFileTypes( inf.getParent(), man.toFile() );

        Assert.assertEquals( "SRP123456789", rm.getStudyId() );
        Assert.assertEquals( "ERS198522", rm.getSampleId() );
        Assert.assertEquals( "ILLUMINA", rm.getPlatform() );
        Assert.assertEquals( "Illumina HiScanSQ", rm.getInstrument() );
        Assert.assertEquals( "CLONEEND", rm.getLibraryStrategy() );
        Assert.assertEquals( "OTHER", rm.getLibrarySource() );
        Assert.assertEquals( "Inverse rRNA selection", rm.getLibrarySelection() );
        Assert.assertEquals( "Name library", rm.getLibraryName() );
        Assert.assertEquals( "library construction protocol", rm.getLibraryConstructionProtocol() );
        Assert.assertEquals( new Integer( 100500 ), rm.getInsertSize() );
        Assert.assertEquals( "SOME-FANCY-NAME", rm.getName() );
        Assert.assertEquals( 2, rm.getFiles().size() );
    }

    
    @Test public void
    testInstrumentUnspecified() throws IOException
    {
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                                ( RawReadsManifestTags.STUDY             + " SRP123456789\n"
                                + RawReadsManifestTags.SAMPLE            + " ERS198522\n"
                                + RawReadsManifestTags.PLATFORM          + " illumina\n"
                                + RawReadsManifestTags.LIBRARY_STRATEGY  + " CLONEEND\n"
                                + RawReadsManifestTags.LIBRARY_SOURCE    + " OTHER\n"
                                + RawReadsManifestTags.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                                + RawReadsManifestTags.NAME              + " SOME-FANCY-NAME\n "
                                + "BAM " + Files.createTempFile( "TEMP", "FILE" ) ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );
        RawReadsManifest rm = new RawReadsManifest();
        rm.defineFileTypes( Paths.get( "." ), man.toFile() );
        Assert.assertEquals( "ILLUMINA", rm.getPlatform() );
        Assert.assertEquals( "unspecified", rm.getInstrument() );
    }
    
    
    @Test public void
    platformOverride() throws IOException
    {
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                                ( RawReadsManifestTags.STUDY             + " SRP123456789\n"
                                + RawReadsManifestTags.SAMPLE            + " ERS198522\n"
                                + RawReadsManifestTags.PLATFORM          + " ILLUMINA\n"
                                + RawReadsManifestTags.INSTRUMENT        + " 454 GS FLX Titanium\n"
                                + RawReadsManifestTags.LIBRARY_STRATEGY  + " CLONEEND\n"
                                + RawReadsManifestTags.LIBRARY_SOURCE    + " OTHER\n"
                                + RawReadsManifestTags.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                                + RawReadsManifestTags.NAME              + " SOME-FANCY-NAME\n "
                                + "BAM " + Files.createTempFile( "TEMP", "FILE" ) ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );
        RawReadsManifest rm = new RawReadsManifest();
        rm.defineFileTypes( Paths.get( "." ), man.toFile() );
        Assert.assertEquals( "LS454", rm.getPlatform() );
        Assert.assertEquals( "454 GS FLX Titanium", rm.getInstrument() );
    }

    
    @Test public void
    instrumentUnspecified() throws IOException
    {
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                                ( RawReadsManifestTags.STUDY             + " SRP123456789\n"
                                + RawReadsManifestTags.SAMPLE            + " ERS198522\n"
                                + RawReadsManifestTags.PLATFORM          + " ILLUMINA\n"
                                + RawReadsManifestTags.INSTRUMENT        + " unspecifieD\n"
                                + RawReadsManifestTags.LIBRARY_STRATEGY  + " CLONEEND\n"
                                + RawReadsManifestTags.LIBRARY_SOURCE    + " OTHER\n"
                                + RawReadsManifestTags.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                                + RawReadsManifestTags.NAME              + " SOME-FANCY-NAME\n "
                                + "BAM " + Files.createTempFile( "TEMP", "FILE" ) ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );
        RawReadsManifest rm = new RawReadsManifest();
        rm.defineFileTypes( Paths.get( "." ), man.toFile() );
        Assert.assertEquals( "ILLUMINA", rm.getPlatform() );
        Assert.assertEquals( "unspecified", rm.getInstrument() );
    }


    @Test( expected=WebinCliException.class ) public void
    instrumentUnspecifiedPlatformMissing() throws IOException
    {
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                                ( RawReadsManifestTags.STUDY             + " SRP123456789\n"
                                + RawReadsManifestTags.SAMPLE            + " ERS198522\n"
                                + RawReadsManifestTags.LIBRARY_STRATEGY  + " CLONEEND\n"
                                + RawReadsManifestTags.LIBRARY_SOURCE    + " OTHER\n"
                                + RawReadsManifestTags.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                                + RawReadsManifestTags.NAME              + " SOME-FANCY-NAME\n "
                                + "BAM " + Files.createTempFile( "TEMP", "FILE" ) ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );
        RawReadsManifest rm = new RawReadsManifest();
        rm.defineFileTypes( Paths.get( "." ), man.toFile() );
        Assert.assertEquals( "ILLUMINA", rm.getPlatform() );
        Assert.assertEquals( "unspecified", rm.getInstrument() );
    }
    

    @Test( expected = WebinCliException.class ) public void
    negativeInsertSize() throws IOException
    {
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                                ( RawReadsManifestTags.STUDY             + " SRP123456789\n"
                                + RawReadsManifestTags.SAMPLE            + " ERS198522\n"
                                + RawReadsManifestTags.PLATFORM          + " ILLUMINA\n"
                                + RawReadsManifestTags.INSTRUMENT        + " unspecifieD\n"
                                + RawReadsManifestTags.INSERT_SIZE       + " -1\n"
                                + RawReadsManifestTags.LIBRARY_STRATEGY  + " CLONEEND\n"
                                + RawReadsManifestTags.LIBRARY_SOURCE    + " OTHER\n"
                                + RawReadsManifestTags.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                                + RawReadsManifestTags.NAME              + " SOME-FANCY-NAME\n "
                                + "BAM " + Files.createTempFile( "TEMP", "FILE" ) ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );
        RawReadsManifest rm = new RawReadsManifest();
        rm.defineFileTypes( Paths.get( "." ), man.toFile() );
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
