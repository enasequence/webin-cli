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

import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.readtools.webin.cli.rawreads.RawReadsFile;
import uk.ac.ebi.ena.readtools.webin.cli.rawreads.RawReadsFile.Filetype;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsManifestReader.Field;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;

import static org.assertj.core.api.Assertions.assertThat;

public class
RawReadsManifestReaderTest
{
    private static final String SAMPLE_ID = "SAMN00001636";
    private static final String STUDY_ID = "PRJEB10672";


    private static RawReadsManifestReader createManifestReader() {
        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setMetadataProcessorsActive(true);
        return new RawReadsManifestReader(ManifestReader.DEFAULT_PARAMETERS, new MetadataProcessorFactory(parameters));
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

        RawReadsFile file = RawReadsWebinCliExecutor.createReadFile(inputDir, new SubmissionFile<>(ReadsManifest.FileType.FASTQ, new File("file.fastq")));
        Assert.assertTrue( file.getFilename().contains( "file.fastq" ) );
        Assert.assertEquals( Filetype.fastq, file.getFiletype() );

        file = RawReadsWebinCliExecutor.createReadFile(inputDir, new SubmissionFile<>(ReadsManifest.FileType.BAM, new File("file.bam")));
        Assert.assertTrue( file.getFilename().contains( "file.bam" ) );
        Assert.assertEquals( Filetype.bam, file.getFiletype() );

        file = RawReadsWebinCliExecutor.createReadFile(inputDir, new SubmissionFile<>(ReadsManifest.FileType.CRAM, new File("file.cram")));
        Assert.assertTrue( file.getFilename().contains( "file.cram" ) );
        Assert.assertEquals( Filetype.cram, file.getFiletype() );
    }

    @Test public void
    testValidManifest() throws IOException
    {
        String descr = "A description";
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                                ( Field.STUDY            + " " + STUDY_ID + "\n"
                                + Field.SAMPLE           + " " + SAMPLE_ID + "\n"
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
        ReadsManifest manifest = rm.getManifest();

        Assert.assertNull( manifest.getStudy() );
        Assert.assertNull( manifest.getSample() );
        Assert.assertNull( manifest.getPlatform() );
        Assert.assertNull( manifest.getInstrument() );
        Assert.assertNull( manifest.getLibraryStrategy() );
        Assert.assertNull( manifest.getLibrarySource() );
        Assert.assertNull( manifest.getLibrarySelection() );
        Assert.assertNull( manifest.getLibraryName() );
        Assert.assertNull( manifest.getLibraryConstructionProtocol() );
        Assert.assertNull( manifest.getInsertSize() );
        Assert.assertNull( manifest.getName() );
        assertThat( manifest.files().files() ).size().isZero();
        Assert.assertNull( manifest.getDescription() );
        
        rm.readManifest( Paths.get( "." ), man.toFile() );

        Assert.assertEquals( STUDY_ID, manifest.getStudy().getBioProjectId() );
        Assert.assertEquals( SAMPLE_ID, manifest.getSample().getBioSampleId() );
        Assert.assertEquals( "ILLUMINA", manifest.getPlatform() );
        Assert.assertEquals( "Illumina HiScanSQ", manifest.getInstrument() );
        Assert.assertEquals( "CLONEEND", manifest.getLibraryStrategy() );
        Assert.assertEquals( "OTHER", manifest.getLibrarySource() );
        Assert.assertEquals( "Inverse rRNA selection", manifest.getLibrarySelection() );
        Assert.assertEquals( "Name library", manifest.getLibraryName() );
        Assert.assertEquals( "library construction protocol", manifest.getLibraryConstructionProtocol() );
        Assert.assertEquals( Integer.valueOf( 100500 ), manifest.getInsertSize() );
        Assert.assertEquals( "SOME-FANCY-NAME", manifest.getName() );
        assertThat( manifest.files().files() ).size().isOne();
        Assert.assertEquals( descr, manifest.getDescription() );
    }


    @Test public void
    testValidManifestWithInfo() throws IOException
    {
        Path inf = Files.write( Files.createTempFile( Files.createTempDirectory( "TEMP" ), "TEMP", "INFO" ),
                                ( Field.STUDY            + " " + STUDY_ID + "\n"
                                + Field.SAMPLE           + " " + SAMPLE_ID + "\n"
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
        ReadsManifest manifest = rm.getManifest();

        Assert.assertNull( manifest.getStudy() );
        Assert.assertNull( manifest.getSample() );
        Assert.assertNull( manifest.getPlatform() );
        Assert.assertNull( manifest.getInstrument() );
        Assert.assertNull( manifest.getLibraryStrategy() );
        Assert.assertNull( manifest.getLibrarySource() );
        Assert.assertNull( manifest.getLibrarySelection() );
        Assert.assertNull( manifest.getLibraryName() );
        Assert.assertNull( manifest.getLibraryConstructionProtocol() );
        Assert.assertNull( manifest.getInsertSize() );
        Assert.assertNull( manifest.getName() );
        assertThat( manifest.files().files() ).size().isZero();

        rm.readManifest( inf.getParent(), man.toFile() );

        Assert.assertEquals( STUDY_ID, manifest.getStudy().getBioProjectId() );
        Assert.assertEquals( SAMPLE_ID, manifest.getSample().getBioSampleId() );
        Assert.assertEquals( "ILLUMINA", manifest.getPlatform() );
        Assert.assertEquals( "Illumina HiScanSQ", manifest.getInstrument() );
        Assert.assertEquals( "CLONEEND", manifest.getLibraryStrategy() );
        Assert.assertEquals( "OTHER", manifest.getLibrarySource() );
        Assert.assertEquals( "Inverse rRNA selection", manifest.getLibrarySelection() );
        Assert.assertEquals( "Name library", manifest.getLibraryName() );
        Assert.assertEquals( "library construction protocol", manifest.getLibraryConstructionProtocol() );
        Assert.assertEquals( new Integer( 100500 ), manifest.getInsertSize() );
        Assert.assertEquals( "SOME-FANCY-NAME", manifest.getName() );
        assertThat( manifest.files().files() ).size().isOne();
    }


    @Test public void
    testUnspecifiedInstrument() throws IOException
    {
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                                ( Field.PLATFORM + " illumina\n" ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        RawReadsManifestReader rm = createManifestReader();
        ReadsManifest manifest = rm.getManifest();

        Assert.assertNull( manifest.getPlatform() );
        Assert.assertNull( manifest.getInstrument() );

        rm.readManifest( Paths.get( "." ), man.toFile() );

        Assert.assertEquals( "ILLUMINA", manifest.getPlatform() );
        Assert.assertEquals( "unspecified", manifest.getInstrument() );
    }

    @Test public void
    testUnspecifiedInstrument2() throws IOException
    {
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ),
                (  Field.PLATFORM + " ILLUMINA\n" +
                Field.INSTRUMENT + " unspecifieD\n" ).getBytes(),
                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        RawReadsManifestReader rm = createManifestReader();
        ReadsManifest manifest = rm.getManifest();

        Assert.assertNull( manifest.getPlatform() );
        Assert.assertNull( manifest.getInstrument() );

        rm.readManifest( Paths.get( "." ), man.toFile() );

        Assert.assertEquals( "ILLUMINA", manifest.getPlatform() );
        Assert.assertEquals( "unspecified", manifest.getInstrument() );
    }

    @Test public void
    testPlatformOverride() throws IOException
    {
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                                ( Field.PLATFORM + " ILLUMINA\n"
                                + Field.INSTRUMENT + " 454 GS FLX Titanium\n" ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        RawReadsManifestReader rm = createManifestReader();
        ReadsManifest manifest = rm.getManifest();

        Assert.assertNull( manifest.getPlatform() );
        Assert.assertNull( manifest.getInstrument() );

        rm.readManifest( Paths.get( "." ), man.toFile() );

        Assert.assertEquals( "LS454", manifest.getPlatform() );
        Assert.assertEquals( "454 GS FLX Titanium", manifest.getInstrument() );
    }


    @Test public void
    testUnspecifiedInstrumentNoPlatform() throws IOException
    {
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                                ( "" ).getBytes(),
                                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        RawReadsManifestReader rm = createManifestReader();
        ReadsManifest manifest = rm.getManifest();

        Assert.assertNull( manifest.getPlatform() );
        Assert.assertNull( manifest.getInstrument() );

        rm.readManifest( Paths.get( "." ), man.toFile() );

        Assert.assertNull( manifest.getPlatform() );
        Assert.assertEquals( "unspecified", manifest.getInstrument() );
        Assert.assertEquals(1, rm.getValidationResult().count(WebinCliMessage.Manifest.MISSING_PLATFORM_AND_INSTRUMENT_ERROR.key(), Severity.ERROR));
    }

    @Test public void
    testUnspecifiedInstrumentNoPlatform2() throws IOException
    {
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ),
                ( Field.INSTRUMENT + " unspecifieD\n" ).getBytes(),
                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        RawReadsManifestReader rm = createManifestReader();
        ReadsManifest manifest = rm.getManifest();

        Assert.assertNull( manifest.getPlatform() );
        Assert.assertNull( manifest.getInstrument() );

        rm.readManifest( Paths.get( "." ), man.toFile() );

        Assert.assertNull( manifest.getPlatform() );
        Assert.assertEquals( "unspecified", manifest.getInstrument() );
        Assert.assertEquals(1, rm.getValidationResult().count(WebinCliMessage.Manifest.MISSING_PLATFORM_AND_INSTRUMENT_ERROR.key(), Severity.ERROR));
    }


    @Test public void
    negativeInsertSize() throws IOException
    {
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                                ( Field.STUDY            + " " +  STUDY_ID + "\n"
                                + Field.SAMPLE           + " " +  SAMPLE_ID + "\n"
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
