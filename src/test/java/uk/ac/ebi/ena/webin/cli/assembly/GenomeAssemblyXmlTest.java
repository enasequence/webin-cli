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
package uk.ac.ebi.ena.webin.cli.assembly;

import java.nio.file.Path;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.WebinCliContext;
import uk.ac.ebi.ena.webin.cli.WebinCliExecutor;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest;
import uk.ac.ebi.ena.webin.cli.validator.reference.Analysis;
import uk.ac.ebi.ena.webin.cli.validator.reference.Run;
import uk.ac.ebi.ena.webin.cli.validator.reference.Study;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class
GenomeAssemblyXmlTest
{
    @Before
    public void
    before()
    {
        Locale.setDefault( Locale.UK );
    }

    private static final String NAME = "test_genome";

    private WebinCliExecutor<GenomeManifest> init() {
        GenomeManifest manifest = new GenomeManifest();
        manifest.setName( NAME );
        manifest.setSample(WebinCliTestUtils.getDefaultSample());
        manifest.getSample().setBioSampleId( "test_sample" );
        manifest.setStudy(new Study());
        manifest.getStudy().setBioProjectId( "test_study" );
        manifest.setCoverage( "1" );
        manifest.setProgram( "test_program" );
        manifest.setPlatform( "test_platform" );

        GenomeAssemblyManifestReader manifestReader = mock(GenomeAssemblyManifestReader.class);
        when(manifestReader.getManifest()).thenReturn(manifest);

        WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
        parameters.setManifestFile( WebinCliTestUtils.createEmptyTempFile().toFile() );
        parameters.setTestMode(false);
        return (WebinCliExecutor<GenomeManifest>) WebinCliContext.genome.createExecutor(parameters, manifestReader);
    }

    @Test public void
    testAnalysisXML_RunAndAnalysisRef()
    {
        WebinCliExecutor<GenomeManifest> executor = init();
        GenomeManifest manifest = executor.getManifestReader().getManifest();

        manifest.addAnalysis(
                new Analysis( "ANALYSIS_ID1", "ANALYSIS_ID1_ALIAS" ),
                new Analysis( "ANALYSIS_ID2", "ANALYSIS_ID2_ALIAS" ) );
        manifest.addRun(
                new Run( "RUN_ID1", "RUN_ID1_ALIAS" ),
                new Run( "RUN_ID2", "RUN_ID2_ALIAS" ) );

        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();

        String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

        WebinCliTestUtils.assertXml(analysisXml,
                "<ANALYSIS_SET>\n"
                      + "  <ANALYSIS>\n"
                      + "    <TITLE>Genome assembly: test_genome</TITLE>\n"
                      + "    <STUDY_REF accession=\"test_study\" />\n"
                      + "    <SAMPLE_REF accession=\"test_sample\" />\n"
                      + "    <RUN_REF accession=\"RUN_ID1\"/>\n"
                      + "    <RUN_REF accession=\"RUN_ID2\"/>\n"
                      + "    <ANALYSIS_REF accession=\"ANALYSIS_ID1\"/>\n"
                      + "    <ANALYSIS_REF accession=\"ANALYSIS_ID2\"/>\n"
                      + "    <ANALYSIS_TYPE>\n"
                      + "      <SEQUENCE_ASSEMBLY>\n"
                      + "        <NAME>test_genome</NAME>\n"
                      + "        <PARTIAL>false</PARTIAL>\n"
                      + "        <COVERAGE>1</COVERAGE>\n"
                      + "        <PROGRAM>test_program</PROGRAM>\n"
                      + "        <PLATFORM>test_platform</PLATFORM>\n"
                      + "      </SEQUENCE_ASSEMBLY>\n"
                      + "    </ANALYSIS_TYPE>\n"
                      + "    <FILES />\n"
                      + "  </ANALYSIS>\n"
                      + "</ANALYSIS_SET>\n");
    }

    @Test public void
    testAnalysisXML_Description()
    {
        WebinCliExecutor<GenomeManifest> executor = init();
        GenomeManifest manifest = executor.getManifestReader().getManifest();

        manifest.setDescription("test_description");

        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();

        String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

        WebinCliTestUtils.assertXml(analysisXml,
                "<ANALYSIS_SET>\n" +
                        "  <ANALYSIS>\n" +
                        "    <TITLE>Genome assembly: test_genome</TITLE>\n" +
                        "    <DESCRIPTION>test_description</DESCRIPTION>\n" +
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
    testAnalysisXML_MolType()
    {
        WebinCliExecutor<GenomeManifest> executor = init();
        GenomeManifest manifest = executor.getManifestReader().getManifest();

        manifest.setMoleculeType("test_moltype");

        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();

        String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

        WebinCliTestUtils.assertXml(analysisXml,
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
                        "        <MOL_TYPE>test_moltype</MOL_TYPE>\n" +
                        "      </SEQUENCE_ASSEMBLY>\n" +
                        "    </ANALYSIS_TYPE>\n" +
                        "    <FILES />\n" +
                        "  </ANALYSIS>\n" +
                        "</ANALYSIS_SET>\n");
    }

    @Test public void
    testAnalysisXML_Tpa()
    {
        WebinCliExecutor<GenomeManifest> executor = init();
        GenomeManifest manifest = executor.getManifestReader().getManifest();

        manifest.setTpa(true);

        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();

        String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

        WebinCliTestUtils.assertXml(analysisXml,
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
    testAnalysisXML_AssemblyType()
    {
        WebinCliExecutor<GenomeManifest> executor = init();
        GenomeManifest manifest = executor.getManifestReader().getManifest();

        manifest.setAssemblyType( "test_assembly_type");

        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();

        String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

        WebinCliTestUtils.assertXml(analysisXml,
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
    testAnalysisXML_FastaFile()
    {
        WebinCliExecutor<GenomeManifest> executor = init();
        GenomeManifest manifest = executor.getManifestReader().getManifest();

        Path fastaFile = WebinCliTestUtils.createGzippedTempFile("flatfile.fasta.gz", ">123\nACGT");
        manifest.files().add( new SubmissionFile( GenomeManifest.FileType.FASTA, fastaFile.toFile() ) );

        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();

        String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

        WebinCliTestUtils.assertXml(analysisXml,
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
                        "      <FILE filename=\"webin-cli/genome/" + NAME + "/" + fastaFile.getFileName() + "\" filetype=\"fasta\" checksum_method=\"MD5\" checksum=\"661926c1c03b059929caaead3ea351a3\" />\n" +
                        "    </FILES>\n" +
                        "  </ANALYSIS>\n" +
                        "</ANALYSIS_SET>\n");
    }

    @Test public void
    testAnalysisXML_FastaFileAndAgpFile()
    {
        WebinCliExecutor<GenomeManifest> executor = init();
        GenomeManifest manifest = executor.getManifestReader().getManifest();

        Path fastaFile = WebinCliTestUtils.createGzippedTempFile("fasta.gz", ">123\nACGT");
        Path agpFile = WebinCliTestUtils.createGzippedTempFile("agp.gz", ">123\nACGT");
        manifest.files().add( new SubmissionFile( GenomeManifest.FileType.FASTA, fastaFile.toFile() ) );
        manifest.files().add( new SubmissionFile( GenomeManifest.FileType.AGP, agpFile.toFile() ) );

        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();

        String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

        WebinCliTestUtils.assertXml(analysisXml,
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
                        "      <FILE filename=\"webin-cli/genome/" + NAME + "/" + fastaFile.getFileName() + "\" filetype=\"fasta\" checksum_method=\"MD5\" checksum=\"661926c1c03b059929caaead3ea351a3\" />\n" +
                        "      <FILE filename=\"webin-cli/genome/" + NAME + "/" + agpFile.getFileName() + "\" filetype=\"agp\" checksum_method=\"MD5\" checksum=\"661926c1c03b059929caaead3ea351a3\" />\n" +
                        "    </FILES>\n" +
                        "  </ANALYSIS>\n" +
                        "</ANALYSIS_SET>\n");
    }

    @Test public void
    testAnalysisXML_AuthorsAndAddress()
    {
        WebinCliExecutor<GenomeManifest> executor = init();
        GenomeManifest manifest = executor.getManifestReader().getManifest();

        manifest.setAuthors( "test_author1,test_author2.");
        manifest.setAddress( "ena,ebi,embl,UK");

        executor.prepareSubmissionBundle();
        SubmissionBundle sb = executor.readSubmissionBundle();

        String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

        WebinCliTestUtils.assertXml(analysisXml,
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
                            "        <AUTHORS>test_author1,test_author2.</AUTHORS>\n"+
                            "        <ADDRESS>ena,ebi,embl,UK</ADDRESS>\n"+
                            "      </SEQUENCE_ASSEMBLY>\n" +
                            "    </ANALYSIS_TYPE>\n" +
                            "    <FILES />\n" +
                            "  </ANALYSIS>\n" +
                            "</ANALYSIS_SET>\n");
    }
}
