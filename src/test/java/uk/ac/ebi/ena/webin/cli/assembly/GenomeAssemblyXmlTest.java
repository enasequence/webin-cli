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
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.embl.api.entry.feature.FeatureFactory;
import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile.FileType;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFiles;
import uk.ac.ebi.embl.api.validation.submission.SubmissionOptions;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.entity.Analysis;
import uk.ac.ebi.ena.webin.cli.entity.Run;
import uk.ac.ebi.ena.webin.cli.entity.Sample;
import uk.ac.ebi.ena.webin.cli.entity.Study;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;

public class
GenomeAssemblyXmlTest
{
    @Before
    public void
    before()
    {
        Locale.setDefault( Locale.UK );
    }
    
    private static SourceFeature getDefaultSourceFeature()
	{
		SourceFeature source= new FeatureFactory().createSourceFeature();
		source.setScientificName("Micrococcus sp. 5");
		return source;
	}

    @Test public void
    testAnalysisXMLAssemblyInfo_WithoutFiles()
    {
        GenomeAssemblyWebinCli cli = new GenomeAssemblyWebinCli();
        String name = "test_genome";
        cli.setName( name );

        AssemblyInfoEntry info = new AssemblyInfoEntry();
        cli.setAssemblyInfo( info );
        info.setName( name );
        info.setBiosampleId( "test_sample" );
        info.setStudyId( "test_study" );
        info.setCoverage( "1" );
        info.setProgram( "test_program" );
        info.setPlatform( "test_platform" );

        cli.setSource(getDefaultSourceFeature());
        cli.setAnalysisRef( new ArrayList<Analysis>() { { add( new Analysis( "ANALYSIS_ID1", "ANALYSIS_ID1_ALIAS" ) ); add( new Analysis( "ANALYSIS_ID2", "ANALYSIS_ID2_ALIAS" ) ); } } );
        cli.setRunRef( new ArrayList<Run>() { { add( new Run( "RUN_ID1", "RUN_ID1_ALIAS" ) ); add( new Run( "RUN_ID2", "RUN_ID2_ALIAS" ) ); } } );
        SubmissionBundle sb = WebinCliTestUtils.prepareSubmissionBundle(cli);

        String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

        WebinCliTestUtils.assertAnalysisXml(analysisXml,
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
    testAnalysisXML_AssemblyInfo_TpaWithoutFiles()
    {
        GenomeAssemblyWebinCli cli = new GenomeAssemblyWebinCli();
        String name = "test_genome";
        cli.setName( name );
        cli.setSource(getDefaultSourceFeature());

        AssemblyInfoEntry info = new AssemblyInfoEntry();
        cli.setAssemblyInfo( info );
        info.setName( name );
        info.setBiosampleId( "test_sample" );
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
    testAnalysisXML_AssemblyInfo_AssemblyTypeWithoutFiles()
    {
        GenomeAssemblyWebinCli cli = new GenomeAssemblyWebinCli();
        String name = "test_genome";
        cli.setName( name );
        cli.setSource(getDefaultSourceFeature());

        AssemblyInfoEntry info = new AssemblyInfoEntry();
        cli.setAssemblyInfo( info );
        info.setName( name );
        info.setBiosampleId( "test_sample" );
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
    testAnalysisXML_AssemblyInfo_WithFastaFile()
    {
    	SubmissionOptions submissionOptions =  new SubmissionOptions();
    	SubmissionFiles submissionFiles = new SubmissionFiles();
        Path fastaFile = WebinCliTestUtils.createGzippedTempFile("flatfile.fasta.gz", ">123\nACGT");
    	SubmissionFile submissionFile = new SubmissionFile(FileType.FASTA,fastaFile.toFile());
    	submissionFiles.addFile(submissionFile);
    	submissionOptions.submissionFiles = Optional.of(submissionFiles);
        GenomeAssemblyWebinCli cli = new GenomeAssemblyWebinCli();
        String name = "test_genome";
        cli.setName( name );
        cli.setSource(getDefaultSourceFeature());
        cli.setSubmissionOptions(submissionOptions);
        cli.getParameters().setInputDir( fastaFile.getParent().toFile() );
        AssemblyInfoEntry info = new AssemblyInfoEntry();
        cli.setAssemblyInfo( info );
        info.setName( name );
        info.setBiosampleId( "test_sample" );
        info.setStudyId( "test_study" );
        info.setProgram( "test_program" );
        info.setPlatform( "test_platform" );
        info.setCoverage("1");
        cli.setDescription( "some test description" );
        SubmissionBundle sb = WebinCliTestUtils.prepareSubmissionBundle(cli);

        String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);
  

        WebinCliTestUtils.assertAnalysisXml(analysisXml,
                "<ANALYSIS_SET>\n" +
                        "  <ANALYSIS>\n" +
                        "    <TITLE>Genome assembly: test_genome</TITLE>\n" +
                        "    <DESCRIPTION>" + cli.getDescription() + "</DESCRIPTION>\n" +
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
                        "      <FILE filename=\"webin-cli/genome/" + name + "/" + fastaFile.getFileName() + "\" filetype=\"fasta\" checksum_method=\"MD5\" checksum=\"661926c1c03b059929caaead3ea351a3\" />\n" +
                        "    </FILES>\n" +
                        "  </ANALYSIS>\n" +
                        "</ANALYSIS_SET>\n");
    }

    @Test public void
    testAnalysisXML_Manifest_WithFastaFile()
    {
        String name = "test_genome";
        Path fastaFile = WebinCliTestUtils.createGzippedTempFile("flatfile.fasta.gz", ">123\nACGT");
        Path inputDir = fastaFile.getParent();
        Path manifestFile = WebinCliTestUtils.createTempFile("manifest.txt", inputDir,
                "NAME\t" + name + "\n" +
                        "SAMPLE\ttest_sample\n" +
                        GenomeAssemblyManifest.Field.DESCRIPTION + " a description\n" +
                        "STUDY\ttest_study\n" +
                        "PROGRAM\ttest_program\n" +
                        "PLATFORM\ttest_platform\n" +
                        "COVERAGE\t1\n" +
                        "FASTA\t" + fastaFile.getFileName() + "\n"
        );

        WebinCliParameters parameters = AssemblyTestUtils.createWebinCliParameters(manifestFile, inputDir);

        GenomeAssemblyWebinCli cli = new GenomeAssemblyWebinCli();
        cli.setSource(getDefaultSourceFeature());
        cli.setMetadataServiceActive(false);
        Sample sample = new Sample();
        sample.setBiosampleId("test_sample");
        cli.setSample(sample);
        Study study = new Study();
        study.setProjectId("test_study");
        cli.setStudy(study);

        try
        {
            cli.readManifest(parameters);
        }
        finally {
            SubmissionBundle sb = WebinCliTestUtils.prepareSubmissionBundle(cli);

            String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

            WebinCliTestUtils.assertAnalysisXml(analysisXml,
                    "<ANALYSIS_SET>\n" +
                            "  <ANALYSIS>\n" +
                            "    <TITLE>Genome assembly: test_genome</TITLE>\n" +
                            "    <DESCRIPTION>" + cli.getDescription() + "</DESCRIPTION>\n" +
                            "    <STUDY_REF accession=\"test_study\" />\n" +
                            "    <SAMPLE_REF accession=\"test_sample\" />\n" +
                            "    <ANALYSIS_TYPE>\n" +
                            "      <SEQUENCE_ASSEMBLY>\n" +
                            "        <NAME>test_genome</NAME>\n" +
                            "        <PARTIAL>false</PARTIAL>\n" +
                            "        <COVERAGE>1</COVERAGE>\n" +
                            "        <PROGRAM>test_program</PROGRAM>\n" +
                            "        <PLATFORM>test_platform</PLATFORM>\n" +
                            "        <MOL_TYPE>genomic DNA</MOL_TYPE>\n" +
                            "      </SEQUENCE_ASSEMBLY>\n" +
                            "    </ANALYSIS_TYPE>\n" +
                            "    <FILES>\n" +
                            "      <FILE filename=\"webin-cli/genome/" + name + "/" + fastaFile.getFileName() + "\" filetype=\"fasta\" checksum_method=\"MD5\" checksum=\"661926c1c03b059929caaead3ea351a3\" />\n" +
                            "    </FILES>\n" +
                            "  </ANALYSIS>\n" +
                            "</ANALYSIS_SET>\n");
        }
    }

    @Test public void
    testAnalysisXMLManifestWithSubmitterReference()
    {
        String name = "test_genome";
        Path fastaFile = WebinCliTestUtils.createGzippedTempFile("flatfile.fasta.gz", ">123\nACGT");
        Path inputDir = fastaFile.getParent();
        Path manifestFile = WebinCliTestUtils.createTempFile("manifest.txt", inputDir,
                "NAME\t" + name + "\n" +
                        "SAMPLE\ttest_sample\n" +
                        GenomeAssemblyManifest.Field.DESCRIPTION + " a description\n" +
                        "STUDY\ttest_study\n" +
                        "PROGRAM\ttest_program\n" +
                        "PLATFORM\ttest_platform\n" +
                        "AUTHORS\ttest_author1,test_author2.\n" +
                        "ADDRESS\tena,ebi,embl,UK\n" +
                        "COVERAGE\t1\n" +
                        "FASTA\t" + fastaFile.getFileName() + "\n"
        );

        WebinCliParameters parameters = AssemblyTestUtils.createWebinCliParameters(manifestFile, inputDir);

        GenomeAssemblyWebinCli cli = new GenomeAssemblyWebinCli();
        cli.setSource(getDefaultSourceFeature());
        cli.setMetadataServiceActive(false);
        Sample sample = new Sample();
        sample.setBiosampleId("test_sample");
        cli.setSample(sample);
        Study study = new Study();
        study.setProjectId("test_study");
        cli.setStudy(study);

        try
        {
            cli.readManifest(parameters);
        }
        finally {
            SubmissionBundle sb = WebinCliTestUtils.prepareSubmissionBundle(cli);

            String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

            WebinCliTestUtils.assertAnalysisXml(analysisXml,
                    "<ANALYSIS_SET>\n" +
                            "  <ANALYSIS>\n" +
                            "    <TITLE>Genome assembly: test_genome</TITLE>\n" +
                            "    <DESCRIPTION>" + cli.getDescription() + "</DESCRIPTION>\n" +
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
                            "        <MOL_TYPE>genomic DNA</MOL_TYPE>\n" +
                            "      </SEQUENCE_ASSEMBLY>\n" +
                            "    </ANALYSIS_TYPE>\n" +
                            "    <FILES>\n" +
                            "      <FILE filename=\"webin-cli/genome/" + name + "/" + fastaFile.getFileName() + "\" filetype=\"fasta\" checksum_method=\"MD5\" checksum=\"661926c1c03b059929caaead3ea351a3\" />\n" +
                            "    </FILES>\n" +
                            "  </ANALYSIS>\n" +
                            "</ANALYSIS_SET>\n");
        }
    }


}
