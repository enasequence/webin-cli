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
import uk.ac.ebi.ena.readtools.webin.cli.rawreads.RawReadsFile;
import uk.ac.ebi.ena.readtools.webin.cli.rawreads.RawReadsFile.Filetype;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.manifest.processor.ProcessorTestUtils;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsManifestReader.Field;

public class
RawReadsManifestReaderTest
{
    private static RawReadsManifestReader createManifestReader() {
        return RawReadsManifestReader.create(ManifestReader.DEFAULT_PARAMETERS, new MetadataProcessorFactory(null));
    }

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

        RawReadsFile file = RawReadsManifestReader.createReadFile(inputDir,
                ProcessorTestUtils.createFieldValue(ManifestFieldType.FILE, Field.FASTQ, "file.fastq"));
        Assert.assertTrue( file.getFilename().contains( "file.fastq" ) );
        Assert.assertEquals( Filetype.fastq, file.getFiletype() );

        file = RawReadsManifestReader.createReadFile(inputDir,
                ProcessorTestUtils.createFieldValue(ManifestFieldType.FILE, Field.BAM, "file.bam"));
        Assert.assertTrue( file.getFilename().contains( "file.bam" ) );
        Assert.assertEquals( Filetype.bam, file.getFiletype() );

        file = RawReadsManifestReader.createReadFile(inputDir,
                ProcessorTestUtils.createFieldValue(ManifestFieldType.FILE, Field.CRAM, "file.cram"));
        Assert.assertTrue( file.getFilename().contains( "file.cram" ) );
        Assert.assertEquals( Filetype.cram, file.getFiletype() );
    }

    @Test public void
    testValidManifest() throws IOException
    {
        String descr = "A description";
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                                ( Field.STUDY             + " SRP123456789\n"
                                + Field.SAMPLE            + " ERS198522\n"
                                + Field.PLATFORM          + " illumina\n"
                                + Field.INSTRUMENT        + " Illumina HiScanSQ\n"
                                + Field.LIBRARY_STRATEGY  + " CLONEEND\n"
                                + Field.LIBRARY_SOURCE    + " OTHER\n"
                                + Field.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                                + Field.LIBRARY_NAME      + " Name library\n"
                                + Field.LIBRARY_CONSTRUCTION_PROTOCOL + " library construction protocol\n"
                                + Field.INSERT_SIZE       + " 100500\n"
                                + Field.NAME              + " SOME-FANCY-NAME\n "
                                + Field.DESCRIPTION       + " " + descr + "\n"
                                + "BAM " + Files.createTempFile( "TEMP", "FILE.bam" ) ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );
        RawReadsManifestReader rm = createManifestReader();
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
        Assert.assertNull( rm.getRawReadFiles() );
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
        Assert.assertEquals( 1, rm.getRawReadFiles().size() );
        Assert.assertEquals( descr, rm.getDescription() );
    }


    @Test public void
    testValidManifestWithInfo() throws IOException
    {
        Path inf = Files.write( Files.createTempFile( Files.createTempDirectory( "TEMP" ), "TEMP", "INFO" ),
                                ( Field.STUDY             + " SRP123456789\n"
                                + Field.SAMPLE            + " ERS198522\n"
                                + Field.PLATFORM          + " illumina\n"
                                + Field.INSTRUMENT        + " Illumina HiScanSQ\n"
                                + Field.LIBRARY_STRATEGY  + " CLONEEND\n"
                                + Field.LIBRARY_SOURCE    + " OTHER\n"
                                + Field.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                                + Field.LIBRARY_NAME      + " Name library\n"
                                + Field.LIBRARY_CONSTRUCTION_PROTOCOL + " library construction protocol\n"
                                + Field.INSERT_SIZE       + " 100500\n"
                                + Field.NAME              + " SOME-FANCY-NAME\n" ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ),
                                ( "INFO " + inf.getFileName() + "\n"
                                + "BAM " + Files.createTempFile( inf.getParent(), "TEMP", "FILE" ).getFileName() ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        RawReadsManifestReader rm = createManifestReader();

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
        Assert.assertNull( rm.getRawReadFiles() );

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
        Assert.assertEquals( 1, rm.getRawReadFiles().size() );
    }


    @Test public void
    testUnspecifiedInstrument() throws IOException
    {
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                                ( Field.PLATFORM + " illumina\n" ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        RawReadsManifestReader rm = createManifestReader();

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
                (  Field.PLATFORM + " ILLUMINA\n" +
                Field.INSTRUMENT + " unspecifieD\n" ).getBytes(),
                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        RawReadsManifestReader rm = createManifestReader();

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
                                ( Field.PLATFORM + " ILLUMINA\n"
                                + Field.INSTRUMENT + " 454 GS FLX Titanium\n" ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        RawReadsManifestReader rm = createManifestReader();

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

        RawReadsManifestReader rm = createManifestReader();

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
                ( Field.INSTRUMENT + " unspecifieD\n" ).getBytes(),
                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        RawReadsManifestReader rm = createManifestReader();

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
                                ( Field.STUDY             + " SRP123456789\n"
                                + Field.SAMPLE            + " ERS198522\n"
                                + Field.PLATFORM          + " ILLUMINA\n"
                                + Field.INSTRUMENT        + " unspecifieD\n"
                                + Field.INSERT_SIZE       + " -1\n"
                                + Field.LIBRARY_STRATEGY  + " CLONEEND\n"
                                + Field.LIBRARY_SOURCE    + " OTHER\n"
                                + Field.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                                + Field.NAME              + " SOME-FANCY-NAME\n "
                                + "BAM " + Files.createTempFile( "TEMP", "FILE" ) ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );
        RawReadsManifestReader rm = createManifestReader();
        rm.readManifest( Paths.get( "." ), man.toFile() );
        Assert.assertEquals(1, rm.getValidationResult().count(WebinCliMessage.Manifest.INVALID_POSITIVE_INTEGER_ERROR.key(), Severity.ERROR));
    }

}
