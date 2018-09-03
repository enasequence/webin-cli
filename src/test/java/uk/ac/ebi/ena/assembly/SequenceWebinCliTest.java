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

package uk.ac.ebi.ena.assembly;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.ena.WebinCliTestUtils;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.AbstractWebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

public class
SequenceWebinCliTest
{
    @Before public void
    before()
    {
        Locale.setDefault( Locale.UK );
    }

    @Test public void
    testAnalysisXML_GenomeAssemblyInfo_WithoutFiles()
    {
        GenomeAssemblyWebinCli cli = new GenomeAssemblyWebinCli();
        String name = "test_genome";
        cli.setName( name );

        AssemblyInfoEntry info = new AssemblyInfoEntry();
        cli.setAssemblyInfo( info );
        info.setName( name );
        info.setSampleId( "test_sample" );
        info.setStudyId( "test_study" );
        info.setCoverage( "1" );
        info.setProgram( "test_program" );
        info.setPlatform( "test_platform" );

        SubmissionBundle sb = WebinCliTestUtils.prepareSubmissionBundle(cli);

        String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

        WebinCliTestUtils.assertAnalysisXml(analysisXml,
                "<ANALYSIS_SET>\n" +
                        "  <ANALYSIS>\n" +
                        "    <TITLE>Genome assembly: test_genome</TITLE>\n" +
                        "    <STUDY_REF accession=\"test_study\" />\n" +
                        "    <SAMPLE_REF accession=\"test_sample\" />\n" +
                        "    <ANALYSIS_TYPE>\n" +
                        "      <SEQUENCE_ASSEMBLY>\n" +
                        "        <NAME>test_genome</NAME>\n" +
                        "        <PARTIAL>false</PARTIAL>\n" +
                        "        <COVERAGE>1</COVERAGE>\n" +
                        "        <PROGRAM>test_program</PROGRAM>\n" +
                        "        <PLATFORM>test_platform</PLATFORM>\n" +
                        "      </SEQUENCE_ASSEMBLY>\n" +
                        "    </ANALYSIS_TYPE>\n" +
                        "    <FILES />\n" +
                        "  </ANALYSIS>\n" +
                        "</ANALYSIS_SET>\n");
    }

    @Test public void
    testAnalysisXML_GenomeAssemblyInfo_TpaWithoutFiles()
    {
        GenomeAssemblyWebinCli cli = new GenomeAssemblyWebinCli();
        String name = "test_genome";
        cli.setName( name );

        AssemblyInfoEntry info = new AssemblyInfoEntry();
        cli.setAssemblyInfo( info );
        info.setName( name );
        info.setSampleId( "test_sample" );
        info.setStudyId( "test_study" );
        info.setCoverage( "1" );
        info.setProgram( "test_program" );
        info.setPlatform( "test_platform" );
        info.setTpa(true);

        SubmissionBundle sb = WebinCliTestUtils.prepareSubmissionBundle(cli);

        String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

        WebinCliTestUtils.assertAnalysisXml(analysisXml,
                "<ANALYSIS_SET>\n" +
                "  <ANALYSIS>\n" +
                "    <TITLE>Genome assembly: test_genome</TITLE>\n" +
                "    <STUDY_REF accession=\"test_study\" />\n" +
                "    <SAMPLE_REF accession=\"test_sample\" />\n" +
                "    <ANALYSIS_TYPE>\n" +
                "      <SEQUENCE_ASSEMBLY>\n" +
                "        <NAME>test_genome</NAME>\n" +
                "        <PARTIAL>false</PARTIAL>\n" +
                "        <COVERAGE>1</COVERAGE>\n" +
                "        <PROGRAM>test_program</PROGRAM>\n" +
                "        <PLATFORM>test_platform</PLATFORM>\n" +
                "        <TPA>true</TPA>\n" +
                "      </SEQUENCE_ASSEMBLY>\n" +
                "    </ANALYSIS_TYPE>\n" +
                "    <FILES />\n" +
                "  </ANALYSIS>\n" +
                "</ANALYSIS_SET>\n");
    }

    @Test public void
    testAnalysisXML_GenomeAssemblyInfo_AssemblyTypeWithoutFiles()
    {
        GenomeAssemblyWebinCli cli = new GenomeAssemblyWebinCli();
        String name = "test_genome";
        cli.setName( name );

        AssemblyInfoEntry info = new AssemblyInfoEntry();
        cli.setAssemblyInfo( info );
        info.setName( name );
        info.setSampleId( "test_sample" );
        info.setStudyId( "test_study" );
        info.setCoverage( "1" );
        info.setProgram( "test_program" );
        info.setPlatform( "test_platform" );
        info.setAssemblyType( "test_assembly_type");

        SubmissionBundle sb = WebinCliTestUtils.prepareSubmissionBundle(cli);

        String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

        WebinCliTestUtils.assertAnalysisXml(analysisXml,
                "<ANALYSIS_SET>\n" +
                        "  <ANALYSIS>\n" +
                        "    <TITLE>Genome assembly: test_genome</TITLE>\n" +
                        "    <STUDY_REF accession=\"test_study\" />\n" +
                        "    <SAMPLE_REF accession=\"test_sample\" />\n" +
                        "    <ANALYSIS_TYPE>\n" +
                        "      <SEQUENCE_ASSEMBLY>\n" +
                        "        <NAME>test_genome</NAME>\n" +
                        "        <TYPE>test_assembly_type</TYPE>\n" +
                        "        <PARTIAL>false</PARTIAL>\n" +
                        "        <COVERAGE>1</COVERAGE>\n" +
                        "        <PROGRAM>test_program</PROGRAM>\n" +
                        "        <PLATFORM>test_platform</PLATFORM>\n" +
                        "      </SEQUENCE_ASSEMBLY>\n" +
                        "    </ANALYSIS_TYPE>\n" +
                        "    <FILES />\n" +
                        "  </ANALYSIS>\n" +
                        "</ANALYSIS_SET>\n");
    }


    @Test public void
    testAnalysisXML_GenomeAssemblyInfo_WithFastaFile()
    {
        GenomeAssemblyWebinCli cli = new GenomeAssemblyWebinCli();
        String name = "test_genome";
        cli.setName( name );

        Path fastaFile = WebinCliTestUtils.createTempFile("flatfile.fasta.gz", true, ">123\nACGT");
        cli.getParameters().setInputDir( fastaFile.getParent().toFile() );
        cli.fastaFiles = Arrays.asList(new File(fastaFile.toString()));

        AssemblyInfoEntry info = new AssemblyInfoEntry();
        cli.setAssemblyInfo( info );
        info.setName( name );
        info.setSampleId( "test_sample" );
        info.setStudyId( "test_study" );
        info.setProgram( "test_program" );
        info.setPlatform( "test_platform" );
        info.setCoverage("1");

        SubmissionBundle sb = WebinCliTestUtils.prepareSubmissionBundle(cli);

        String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

        WebinCliTestUtils.assertAnalysisXml(analysisXml,
                "<ANALYSIS_SET>\n" +
                        "  <ANALYSIS>\n" +
                        "    <TITLE>Genome assembly: test_genome</TITLE>\n" +
                        "    <STUDY_REF accession=\"test_study\" />\n" +
                        "    <SAMPLE_REF accession=\"test_sample\" />\n" +
                        "    <ANALYSIS_TYPE>\n" +
                        "      <SEQUENCE_ASSEMBLY>\n" +
                        "        <NAME>test_genome</NAME>\n" +
                        "        <PARTIAL>false</PARTIAL>\n" +
                        "        <COVERAGE>1</COVERAGE>\n" +
                        "        <PROGRAM>test_program</PROGRAM>\n" +
                        "        <PLATFORM>test_platform</PLATFORM>\n" +
                        "      </SEQUENCE_ASSEMBLY>\n" +
                        "    </ANALYSIS_TYPE>\n" +
                        "    <FILES>\n" +
                        "      <FILE filename=\"genome/" + name + "/" + fastaFile.getFileName() + "\" filetype=\"fasta\" checksum_method=\"MD5\" checksum=\"661926c1c03b059929caaead3ea351a3\" />\n" +
                        "    </FILES>\n" +
                        "  </ANALYSIS>\n" +
                        "</ANALYSIS_SET>\n");
    }

    @Test public void
    testAnalysisXML_GenomeManifestParsing_WithFastaFile()
    {
        String name = "test_genome";
        Path fastaFile = WebinCliTestUtils.createTempFile("flatfile.fasta.gz", true, ">123\nACGT");
        Path inputDir = fastaFile.getParent();
        Path manifestFile = WebinCliTestUtils.createTempFile("manifest.txt", inputDir, false,
                "NAME\t" + name + "\n" +
                "SAMPLE\ttest_sample\n" +
                "STUDY\ttest_study\n" +
                "PROGRAM\ttest_program\n" +
                "PLATFORM\ttest_platform\n" +
                "COVERAGE\t1\n" +
                "FASTA\t" + fastaFile.getFileName() + "\n"
        );

        WebinCliParameters parameters = WebinCliTestUtils.createWebinCliParameters(manifestFile, inputDir);

        GenomeAssemblyWebinCli cli = new GenomeAssemblyWebinCli();

        cli.setFetchSample(false);
        Sample sample = new Sample();
        sample.setBiosampleId("test_sample");
        cli.setSample(sample);

        cli.setFetchStudy(false);
        Study study = new Study();
        study.setProjectId("test_study");
        cli.setStudy(study);

        try
        {
            cli.init(parameters);
        }
        finally {
            SubmissionBundle sb = WebinCliTestUtils.prepareSubmissionBundle(cli);

            String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

            WebinCliTestUtils.assertAnalysisXml(analysisXml,
                    "<ANALYSIS_SET>\n" +
                            "  <ANALYSIS>\n" +
                            "    <TITLE>Genome assembly: test_genome</TITLE>\n" +
                            "    <STUDY_REF accession=\"test_study\" />\n" +
                            "    <SAMPLE_REF accession=\"test_sample\" />\n" +
                            "    <ANALYSIS_TYPE>\n" +
                            "      <SEQUENCE_ASSEMBLY>\n" +
                            "        <NAME>test_genome</NAME>\n" +
                            "        <PARTIAL>false</PARTIAL>\n" +
                            "        <COVERAGE>1</COVERAGE>\n" +
                            "        <PROGRAM>test_program</PROGRAM>\n" +
                            "        <PLATFORM>test_platform</PLATFORM>\n" +
                            "      </SEQUENCE_ASSEMBLY>\n" +
                            "    </ANALYSIS_TYPE>\n" +
                            "    <FILES>\n" +
                            "      <FILE filename=\"genome/" + name + "/" + fastaFile.getFileName() + "\" filetype=\"fasta\" checksum_method=\"MD5\" checksum=\"661926c1c03b059929caaead3ea351a3\" />\n" +
                            "    </FILES>\n" +
                            "  </ANALYSIS>\n" +
                            "</ANALYSIS_SET>\n");
        }
    }




    @Test public void
    testAnalysisXML_TranscriptomeAssemblyInfo_WithFastaFile()
    {
        TranscriptomeAssemblyWebinCli cli = new TranscriptomeAssemblyWebinCli();
        String name = "test_transcriptome";
        cli.setName( name );

        Path fastaFile = WebinCliTestUtils.createTempFile(false, ">123\nACGT");
        cli.getParameters().setInputDir( fastaFile.getParent().toFile() );
        cli.fastaFiles = Arrays.asList(new File(fastaFile.toString()));

        AssemblyInfoEntry info = new AssemblyInfoEntry();
        cli.setAssemblyInfo( info );
        info.setName( name );
        info.setSampleId( "test_sample" );
        info.setStudyId( "test_study" );
        info.setProgram( "test_program" );
        info.setPlatform( "test_platform" );

        SubmissionBundle sb = WebinCliTestUtils.prepareSubmissionBundle(cli);

        String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

        WebinCliTestUtils.assertAnalysisXml(analysisXml,
                "<ANALYSIS_SET>\n" +
                        "  <ANALYSIS>\n" +
                        "    <TITLE>Transcriptome assembly: test_transcriptome</TITLE>\n" +
                        "    <STUDY_REF accession=\"test_study\" />\n" +
                        "    <SAMPLE_REF accession=\"test_sample\" />\n" +
                        "    <ANALYSIS_TYPE>\n" +
                        "      <TRANSCRIPTOME_ASSEMBLY>\n" +
                        "        <NAME>test_transcriptome</NAME>\n" +
                        "        <PROGRAM>test_program</PROGRAM>\n" +
                        "        <PLATFORM>test_platform</PLATFORM>\n" +
                        "      </TRANSCRIPTOME_ASSEMBLY>\n" +
                        "    </ANALYSIS_TYPE>\n" +
                        "    <FILES>\n" +
                        "      <FILE filename=\"transcriptome/" + name + "/" + fastaFile.getFileName() + "\" filetype=\"fasta\" checksum_method=\"MD5\" checksum=\"6f82bc96add84ece757afad265d7e341\" />\n" +
                        "    </FILES>\n" +
                        "  </ANALYSIS>\n" +
                        "</ANALYSIS_SET>\n");
    }

    @Test public void
    testAnalysisXML_TranscriptomeManifestParsing_WithFlatFile()
    {
        String name = "test_transcriptome";
        Path flatFile = WebinCliTestUtils.createTempFile("flatfile.dat.gz", true, "ID   ;");
        Path inputDir = flatFile.getParent();
        Path manifestFile = WebinCliTestUtils.createTempFile("manifest.txt", inputDir, false,
                "NAME\t" + name + "\n" +
                "SAMPLE\ttest_sample\n" +
                "STUDY\ttest_study\n" +
                "PROGRAM\ttest_program\n" +
                "PLATFORM\ttest_platform\n" +
                "FLATFILE\t" + flatFile.getFileName() + "\n"
        );

        WebinCliParameters parameters = WebinCliTestUtils.createWebinCliParameters(manifestFile, inputDir);

        TranscriptomeAssemblyWebinCli cli = new TranscriptomeAssemblyWebinCli();

        cli.setFetchSample(false);
        Sample sample = new Sample();
        sample.setBiosampleId("test_sample");
        cli.setSample(sample);

        cli.setFetchStudy(false);
        Study study = new Study();
        study.setProjectId("test_study");
        cli.setStudy(study);

        try
        {
            cli.init(parameters);
        }
        finally {
            SubmissionBundle sb = WebinCliTestUtils.prepareSubmissionBundle(cli);

            String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

            WebinCliTestUtils.assertAnalysisXml(analysisXml,
                    "<ANALYSIS_SET>\n" +
                            "<ANALYSIS>\n" +
                            "<TITLE>Transcriptome assembly: test_transcriptome</TITLE>\n" +
                            "<STUDY_REF accession=\"test_study\"/>\n" +
                            "<SAMPLE_REF accession=\"test_sample\"/>\n" +
                            "<ANALYSIS_TYPE>\n" +
                            "<TRANSCRIPTOME_ASSEMBLY>\n" +
                            "<NAME>test_transcriptome</NAME>\n" +
                            "<PROGRAM>test_program</PROGRAM>\n" +
                            "<PLATFORM>test_platform</PLATFORM>\n" +
                            "</TRANSCRIPTOME_ASSEMBLY>\n" +
                            "</ANALYSIS_TYPE>\n" +
                            "<FILES>\n" +
                            "      <FILE filename=\"transcriptome/" + name + "/" + flatFile.getFileName() + "\" filetype=\"flatfile\" checksum_method=\"MD5\" checksum=\"e334ca8a758084ba2f9f5975e798039e\" />\n" +
                            "</FILES>\n" +
                            "</ANALYSIS>\n" +
                            "</ANALYSIS_SET>");
        }
    }




    @Test public void
    testAnalysisXML_SequenceAssemblyInfo_WithFlatFile()
    {
        SequenceAssemblyWebinCli cli = new SequenceAssemblyWebinCli();
        String name = "test_sequence";
        cli.setName( name );

        Path flatFile = WebinCliTestUtils.createTempFile("flatfile.dat.gz", true, "ID   ;");
        cli.getParameters().setInputDir( flatFile.getParent().toFile() );
        cli.flatFiles = Arrays.asList(new File(flatFile.toString()));

        AssemblyInfoEntry info = new AssemblyInfoEntry();
        cli.setAssemblyInfo( info );
        info.setName( name );
        info.setStudyId( "test_study" );

        SubmissionBundle sb = WebinCliTestUtils.prepareSubmissionBundle(cli);

        String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

        WebinCliTestUtils.assertAnalysisXml(analysisXml,
                "<ANALYSIS_SET>\n" +
                        "<ANALYSIS>\n" +
                        "<TITLE>Sequence assembly: test_sequence</TITLE>\n" +
                        "<STUDY_REF accession=\"test_study\"/>\n" +
                        "<ANALYSIS_TYPE>\n" +
                        "<SEQUENCE_FLATFILE/>\n" +
                        "</ANALYSIS_TYPE>\n" +
                        "<FILES>\n" +
                        "      <FILE filename=\"sequence/" + name + "/" + flatFile.getFileName() + "\" filetype=\"flatfile\" checksum_method=\"MD5\" checksum=\"e334ca8a758084ba2f9f5975e798039e\" />\n" +
                        "</FILES>\n" +
                        "</ANALYSIS>\n" +
                        "</ANALYSIS_SET>");
    }

    @Test public void
    testAnalysisXML_SequenceManifestParsing_WithFlatFile()
    {
        String name = "test_sequence";
        Path flatFile = WebinCliTestUtils.createTempFile("flatfile.dat.gz", true, "ID   ;");
        Path inputDir = flatFile.getParent();
        Path manifestFile = WebinCliTestUtils.createTempFile("manifest.txt", inputDir, false,
                "NAME\t" + name + "\n" +
                "STUDY\ttest_study\n" +
                "FLATFILE\t" + flatFile.getFileName() + "\n"
        );

        WebinCliParameters parameters = WebinCliTestUtils.createWebinCliParameters(manifestFile, inputDir);

        SequenceAssemblyWebinCli cli = new SequenceAssemblyWebinCli();

        cli.setFetchStudy(false);
        Study study = new Study();
        study.setProjectId("test_study");
        cli.setStudy(study);

        try
        {
            cli.init(parameters);
        }
        finally {
            SubmissionBundle sb = WebinCliTestUtils.prepareSubmissionBundle(cli);

            String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

            WebinCliTestUtils.assertAnalysisXml(analysisXml,
                    "<ANALYSIS_SET>\n" +
                            "<ANALYSIS>\n" +
                            "<TITLE>Sequence assembly: test_sequence</TITLE>\n" +
                            "<STUDY_REF accession=\"test_study\"/>\n" +
                            "<ANALYSIS_TYPE>\n" +
                            "<SEQUENCE_FLATFILE/>\n" +
                            "</ANALYSIS_TYPE>\n" +
                            "<FILES>\n" +
                            "      <FILE filename=\"sequence/" + name + "/" + flatFile.getFileName() + "\" filetype=\"flatfile\" checksum_method=\"MD5\" checksum=\"e334ca8a758084ba2f9f5975e798039e\" />\n" +
                            "</FILES>\n" +
                            "</ANALYSIS>\n" +
                            "</ANALYSIS_SET>");
        }
    }






    // Testing manifest parsing of file fields
    //


    @Test( expected = WebinCliException.class )
    public void testGenomeFastaFileField() throws Exception
    {
        GenomeAssemblyWebinCli webinCli = new GenomeAssemblyWebinCli();
        try {
            testManifestParsingFileField(webinCli, "FASTA", "test.fasta.gz" );
        }
        finally {
            Assert.assertEquals(1, webinCli.fastaFiles.size());
            Assert.assertEquals(0, webinCli.agpFiles.size());
            Assert.assertEquals(0, webinCli.flatFiles.size());
            Assert.assertNull(webinCli.chromosomeListFile);
            Assert.assertNull(webinCli.unlocalisedListFile);
        }
    }

    @Test( expected = WebinCliException.class )
    public void testGenome_ManifestParsing_FlatFileField() throws Exception
    {
        GenomeAssemblyWebinCli webinCli = new GenomeAssemblyWebinCli();
        try {
            testManifestParsingFileField(webinCli, "FLATFILE", "test.dat.gz" );
        }
        finally {
            Assert.assertEquals(0, webinCli.fastaFiles.size());
            Assert.assertEquals(0, webinCli.agpFiles.size());
            Assert.assertEquals(1, webinCli.flatFiles.size());
            Assert.assertNull(webinCli.chromosomeListFile);
            Assert.assertNull(webinCli.unlocalisedListFile);
        }
    }

    @Test( expected = WebinCliException.class )
    public void testGenome_ManifestParsing_AgpFileField() throws Exception
    {
        GenomeAssemblyWebinCli webinCli = new GenomeAssemblyWebinCli();
        try {
            testManifestParsingFileField(webinCli, "AGP", "test.agp.gz" );
        }
        finally {
            Assert.assertEquals(0, webinCli.fastaFiles.size());
            Assert.assertEquals(1, webinCli.agpFiles.size());
            Assert.assertEquals(0, webinCli.flatFiles.size());
            Assert.assertNull(webinCli.chromosomeListFile);
            Assert.assertNull(webinCli.unlocalisedListFile);
        }
    }

    @Test( expected = WebinCliException.class )
    public void testGenome_ManifestParsing_ChromosomeListFileField() throws Exception
    {
        GenomeAssemblyWebinCli webinCli = new GenomeAssemblyWebinCli();
        try {
            testManifestParsingFileField(webinCli, "CHROMOSOME_LIST", "test.txt.gz" );
        }
        finally {
            Assert.assertEquals(0, webinCli.fastaFiles.size());
            Assert.assertEquals(0, webinCli.agpFiles.size());
            Assert.assertEquals(0, webinCli.flatFiles.size());
            Assert.assertNotNull(webinCli.chromosomeListFile);
            Assert.assertNull(webinCli.unlocalisedListFile);
        }
    }

    @Test( expected = WebinCliException.class )
    public void testGenome_ManifestParsing_UnlocalisedListFileField() throws Exception
    {
        GenomeAssemblyWebinCli webinCli = new GenomeAssemblyWebinCli();
        try {
            testManifestParsingFileField(webinCli, "UNLOCALISED_LIST", "test.txt.gz" );
        }
        finally {
            Assert.assertEquals(0, webinCli.fastaFiles.size());
            Assert.assertEquals(0, webinCli.agpFiles.size());
            Assert.assertEquals(0, webinCli.flatFiles.size());
            Assert.assertNull(webinCli.chromosomeListFile);
            Assert.assertNotNull(webinCli.unlocalisedListFile);
        }
    }

    @Test( expected = WebinCliException.class )
    public void testTranscriptome_ManifestParsing_FastaFileField() throws Exception
    {
        TranscriptomeAssemblyWebinCli webinCli = new TranscriptomeAssemblyWebinCli();
        try {
            testManifestParsingFileField(webinCli, "FASTA", "test.fasta.gz" );
        }
        finally {
            Assert.assertEquals(1, webinCli.fastaFiles.size());
            Assert.assertEquals(0, webinCli.flatFiles.size());
        }
    }

    @Test( expected = WebinCliException.class )
    public void testSequence_ManifestParsing_FlatFileField() throws Exception
    {
        SequenceAssemblyWebinCli webinCli = new SequenceAssemblyWebinCli();
        try {
            testManifestParsingFileField(webinCli, "FLATFILE", "test.dat.gz" );
        }
        finally {
            Assert.assertEquals(0, webinCli.tsvFiles.size());
            Assert.assertEquals(1, webinCli.flatFiles.size());
        }
    }

    @Test( expected = WebinCliException.class )
    public void testSequence_ManifestParsing_TabFileField() throws Exception
    {
        SequenceAssemblyWebinCli webinCli = new SequenceAssemblyWebinCli();
        try {
            testManifestParsingFileField(webinCli, "TAB", "test.dat.gz" );
        }
        finally {
            Assert.assertEquals(1, webinCli.tsvFiles.size());
            Assert.assertEquals(0, webinCli.flatFiles.size());
        }
    }

    @Test( expected = WebinCliException.class )
    public void testTranscriptome_ManifestParsing_FlatFileField() throws Exception
    {
        TranscriptomeAssemblyWebinCli webinCli = new TranscriptomeAssemblyWebinCli();
        try {
            testManifestParsingFileField(webinCli, "FLATFILE", "test.dat.gz" );
        }
        finally {
            Assert.assertEquals(0, webinCli.fastaFiles.size());
            Assert.assertEquals(1, webinCli.flatFiles.size());
        }
    }



    private void testManifestParsingFileField(AbstractWebinCli webinCli, String fieldName, String fileName) throws Exception
    {
        Path inputDir = WebinCliTestUtils.createOutputFolder().toPath();
        Path filePath = WebinCliTestUtils.createDefaultTempFile( fileName, inputDir, true );

        Path manifestFilePath = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ),
                (fieldName + " " + inputDir.relativize( filePath ).toString() ).getBytes(),
                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        WebinCliParameters parameters = new WebinCliParameters();

        parameters.setManifestFile( manifestFilePath.toFile() );
        parameters.setInputDir( inputDir.toFile() );
        parameters.setOutputDir( WebinCliTestUtils.createOutputFolder() );

        try {
            webinCli.init(parameters);
        }
        finally {
            Assert.assertFalse(webinCli.getManifestReader().getValidationResult().isValid());
        }
    }
}
