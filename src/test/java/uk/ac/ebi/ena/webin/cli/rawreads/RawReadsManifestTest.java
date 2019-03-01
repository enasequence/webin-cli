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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldDefinition;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsFile.Filetype;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsManifest.Fields;

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
    testCreateReadFile()
    {
        Path inputDir = Paths.get( "." );

        RawReadsFile file = RawReadsManifest.createReadFile(inputDir, new ManifestFieldValue(
                new ManifestFieldDefinition(Fields.FASTQ, ManifestFieldType.FILE, 0, 2), "file.fastq", null));
        Assert.assertTrue( file.getFilename().contains( "file.fastq" ) );
        Assert.assertEquals( Filetype.fastq, file.getFiletype() );

        file = RawReadsManifest.createReadFile(inputDir, new ManifestFieldValue(
                new ManifestFieldDefinition(Fields.BAM, ManifestFieldType.FILE, 0, 1), "file.bam", null));
        Assert.assertTrue( file.getFilename().contains( "file.bam" ) );
        Assert.assertEquals( Filetype.bam, file.getFiletype() );

        file = RawReadsManifest.createReadFile(inputDir, new ManifestFieldValue(
                new ManifestFieldDefinition(Fields.CRAM, ManifestFieldType.FILE, 0, 1), "file.cram", null));
        Assert.assertTrue( file.getFilename().contains( "file.cram" ) );
        Assert.assertEquals( Filetype.cram, file.getFiletype() );
    }

    @Test public void
    testValidManifest() throws IOException
    {
        String descr = "A description";
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                                ( Fields.STUDY             + " SRP123456789\n"
                                + Fields.SAMPLE            + " ERS198522\n"
                                + Fields.PLATFORM          + " illumina\n"
                                + Fields.INSTRUMENT        + " Illumina HiScanSQ\n"
                                + Fields.LIBRARY_STRATEGY  + " CLONEEND\n"
                                + Fields.LIBRARY_SOURCE    + " OTHER\n"
                                + Fields.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                                + Fields.LIBRARY_NAME      + " Name library\n"
                                + Fields.LIBRARY_CONSTRUCTION_PROTOCOL + " library construction protocol\n"
                                + Fields.INSERT_SIZE       + " 100500\n"
                                + Fields.NAME              + " SOME-FANCY-NAME\n "
                                + Fields.DESCRIPTION       + " " + descr + "\n"
                                + "BAM " + Files.createTempFile( "TEMP", "FILE.bam" ) ).getBytes(),
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
        Assert.assertNull( rm.getDescription() );
        
        rm.readManifest( Paths.get( "." ), man.toFile() );

        Assert.assertEquals( "SRP123456789", rm.getStudyId() );
        Assert.assertEquals( "ERS198522", rm.getSampleId() );
        Assert.assertEquals( "ILLUMINA", rm.getPlatform() );
        Assert.assertEquals( "Illumina HiScanSQ", rm.getInstrument() );
        Assert.assertEquals( "CLONEEND", rm.getLibraryStrategy() );
        Assert.assertEquals( "OTHER", rm.getLibrarySource() );
        Assert.assertEquals( "Inverse rRNA selection", rm.getLibrarySelection() );
        Assert.assertEquals( "Name library", rm.getLibraryName() );
        Assert.assertEquals( "library construction protocol", rm.getLibraryConstructionProtocol() );
        Assert.assertEquals( Integer.valueOf( 100500 ), rm.getInsertSize() );
        Assert.assertEquals( "SOME-FANCY-NAME", rm.getName() );
        Assert.assertEquals( 1, rm.getFiles().size() );
        Assert.assertEquals( descr, rm.getDescription() );
    }


    @Test public void
    testValidManifestWithInfo() throws IOException
    {
        Path inf = Files.write( Files.createTempFile( Files.createTempDirectory( "TEMP" ), "TEMP", "INFO" ),
                                ( Fields.STUDY             + " SRP123456789\n"
                                + Fields.SAMPLE            + " ERS198522\n"
                                + Fields.PLATFORM          + " illumina\n"
                                + Fields.INSTRUMENT        + " Illumina HiScanSQ\n"
                                + Fields.LIBRARY_STRATEGY  + " CLONEEND\n"
                                + Fields.LIBRARY_SOURCE    + " OTHER\n"
                                + Fields.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                                + Fields.LIBRARY_NAME      + " Name library\n"
                                + Fields.LIBRARY_CONSTRUCTION_PROTOCOL + " library construction protocol\n"
                                + Fields.INSERT_SIZE       + " 100500\n"
                                + Fields.NAME              + " SOME-FANCY-NAME\n" ).getBytes(),
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

        rm.readManifest( inf.getParent(), man.toFile() );

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
    testUnspecifiedInstrument() throws IOException
    {
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                                ( Fields.PLATFORM + " illumina\n" ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        RawReadsManifest rm = new RawReadsManifest();

        Assert.assertNull( rm.getPlatform() );
        Assert.assertNull( rm.getInstrument() );

        rm.readManifest( Paths.get( "." ), man.toFile() );

        Assert.assertEquals( "ILLUMINA", rm.getPlatform() );
        Assert.assertEquals( "unspecified", rm.getInstrument() );
    }

    @Test public void
    testUnspecifiedInstrument2() throws IOException
    {
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ),
                (  Fields.PLATFORM + " ILLUMINA\n" +
                Fields.INSTRUMENT + " unspecifieD\n" ).getBytes(),
                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        RawReadsManifest rm = new RawReadsManifest();

        Assert.assertNull( rm.getPlatform() );
        Assert.assertNull( rm.getInstrument() );

        rm.readManifest( Paths.get( "." ), man.toFile() );

        Assert.assertEquals( "ILLUMINA", rm.getPlatform() );
        Assert.assertEquals( "unspecified", rm.getInstrument() );
    }

    @Test public void
    testPlatformOverride() throws IOException
    {
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                                ( Fields.PLATFORM + " ILLUMINA\n"
                                + Fields.INSTRUMENT + " 454 GS FLX Titanium\n" ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        RawReadsManifest rm = new RawReadsManifest();

        Assert.assertNull( rm.getPlatform() );
        Assert.assertNull( rm.getInstrument() );

        rm.readManifest( Paths.get( "." ), man.toFile() );

        Assert.assertEquals( "LS454", rm.getPlatform() );
        Assert.assertEquals( "454 GS FLX Titanium", rm.getInstrument() );
    }


    @Test public void
    testUnspecifiedInstrumentNoPlatform() throws IOException
    {
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                                ( "" ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        RawReadsManifest rm = new RawReadsManifest();

        Assert.assertNull( rm.getPlatform() );
        Assert.assertNull( rm.getInstrument() );

        rm.readManifest( Paths.get( "." ), man.toFile() );

        Assert.assertNull( rm.getPlatform() );
        Assert.assertEquals( "unspecified", rm.getInstrument() );
        Assert.assertEquals(1, rm.getValidationResult().count(WebinCliMessage.Manifest.MISSING_PLATFORM_AND_INSTRUMENT_ERROR.key(), Severity.ERROR));
    }

    @Test public void
    testUnspecifiedInstrumentNoPlatform2() throws IOException
    {
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ),
                ( Fields.INSTRUMENT + " unspecifieD\n" ).getBytes(),
                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        RawReadsManifest rm = new RawReadsManifest();

        Assert.assertNull( rm.getPlatform() );
        Assert.assertNull( rm.getInstrument() );

        rm.readManifest( Paths.get( "." ), man.toFile() );

        Assert.assertNull( rm.getPlatform() );
        Assert.assertEquals( "unspecified", rm.getInstrument() );
        Assert.assertEquals(1, rm.getValidationResult().count(WebinCliMessage.Manifest.MISSING_PLATFORM_AND_INSTRUMENT_ERROR.key(), Severity.ERROR));
    }


    @Test public void
    negativeInsertSize() throws IOException
    {
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                                ( Fields.STUDY             + " SRP123456789\n"
                                + Fields.SAMPLE            + " ERS198522\n"
                                + Fields.PLATFORM          + " ILLUMINA\n"
                                + Fields.INSTRUMENT        + " unspecifieD\n"
                                + Fields.INSERT_SIZE       + " -1\n"
                                + Fields.LIBRARY_STRATEGY  + " CLONEEND\n"
                                + Fields.LIBRARY_SOURCE    + " OTHER\n"
                                + Fields.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                                + Fields.NAME              + " SOME-FANCY-NAME\n "
                                + "BAM " + Files.createTempFile( "TEMP", "FILE" ) ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );
        RawReadsManifest rm = new RawReadsManifest();
        rm.readManifest( Paths.get( "." ), man.toFile() );
        Assert.assertEquals(1, rm.getValidationResult().count(WebinCliMessage.Manifest.INVALID_POSITIVE_INTEGER_ERROR.key(), Severity.ERROR));
    }

}
