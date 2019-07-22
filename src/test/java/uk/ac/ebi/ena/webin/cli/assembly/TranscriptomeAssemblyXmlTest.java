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
TranscriptomeAssemblyXmlTest {
    @Before
    public void
    before() {
        Locale.setDefault(Locale.UK);
    }

    
    @Test public void
    testAnalysisXML_AssemblyInfo_WithFastaFile() 
    {
    	Path fastaFile = WebinCliTestUtils.createTempFile( ">123\nACGT" );
    	SubmissionOptions submissionOptions =  new SubmissionOptions();
    	SubmissionFiles submissionFiles = new SubmissionFiles();
    	SubmissionFile submissionFile = new SubmissionFile( FileType.FASTA,fastaFile.toFile() );
    	submissionFiles.addFile( submissionFile );
    	submissionOptions.submissionFiles = Optional.of( submissionFiles );
        TranscriptomeAssemblyWebinCli cli = new TranscriptomeAssemblyWebinCli();
        String name = "test_transcriptome";
        cli.setName( name );
        cli.getParameters().setInputDir( fastaFile.getParent().toFile() );
        AssemblyInfoEntry info = new AssemblyInfoEntry();
        cli.setAssemblyInfo( info );
        cli.setSubmissionOptions( submissionOptions );
        info.setName( name );
        info.setBiosampleId( "test_sample" );
        info.setStudyId( "test_study" );
        info.setProgram( "test_program" );
        info.setPlatform( "test_platform" );
        info.setTpa( false );
        cli.setDescription( "test description" );
        
        cli.setAnalysisRef( new ArrayList<Analysis>() { { add( new Analysis( "ANALYSIS_ID1", "ANALYSIS_ID1_ALIAS" ) ); add( new Analysis( "ANALYSIS_ID2", "ANALYSIS_ID2_ALIAS" ) ); } } );
        cli.setRunRef( new ArrayList<Run>() { { add( new Run( "RUN_ID1", "RUN_ID1_ALIAS" ) ); add( new Run( "RUN_ID2", "RUN_ID2_ALIAS" ) ); } } );
        
        SubmissionBundle sb = WebinCliTestUtils.prepareSubmissionBundle( cli );

        String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle( sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS );

        WebinCliTestUtils.assertAnalysisXml( analysisXml,
                "<ANALYSIS_SET>\n"
                      + "  <ANALYSIS>\n"
                      + "    <TITLE>Transcriptome assembly: test_transcriptome</TITLE>\n"
                      + "    <DESCRIPTION>" + cli.getDescription() + "</DESCRIPTION>\n"
                      + "    <STUDY_REF accession=\"test_study\" />\n"
                      + "    <SAMPLE_REF accession=\"test_sample\" />\n"
                      + "    <RUN_REF accession=\"RUN_ID1\"/>\n"
                      + "    <RUN_REF accession=\"RUN_ID2\"/>\n"
                      + "    <ANALYSIS_REF accession=\"ANALYSIS_ID1\"/>\n"
                      + "    <ANALYSIS_REF accession=\"ANALYSIS_ID2\"/>\n"
                      + "    <ANALYSIS_TYPE>\n"
                      + "      <TRANSCRIPTOME_ASSEMBLY>\n"
                      + "        <NAME>test_transcriptome</NAME>\n"
                      + "        <PROGRAM>test_program</PROGRAM>\n"
                      + "        <PLATFORM>test_platform</PLATFORM>\n"
                      + "      </TRANSCRIPTOME_ASSEMBLY>\n"
                      + "    </ANALYSIS_TYPE>\n"
                      + "    <FILES>\n"
                      + "      <FILE filename=\"webin-cli/transcriptome/" + name + "/" + fastaFile.getFileName() + "\" filetype=\"fasta\" checksum_method=\"MD5\" checksum=\"6f82bc96add84ece757afad265d7e341\" />\n"
                      + "    </FILES>\n"
                      + "  </ANALYSIS>\n"
                      + "</ANALYSIS_SET>\n" );
    }

    @Test
    public void
    testAnalysisXML_Manifest_WithFlatFile() {
        String name       = "test_transcriptome";
        Path flatFile     = WebinCliTestUtils.createGzippedTempFile("flatfile.dat.gz", "ID   ;");
        Path inputDir     = flatFile.getParent();
        String descr      = "some-descr";
        Path manifestFile = WebinCliTestUtils.createTempFile( "manifest.txt", inputDir,
                "NAME\t" + name + "\n"
                                                            + "DESCRIPTION " + descr   + "\n"
                                                            + "SAMPLE\ttest_sample\n"
                                                            + "STUDY\ttest_study\n"
                                                            + "PROGRAM\ttest_program\n"
                                                            + "PLATFORM\ttest_platform\n"
                                                            +"AUTHORS\ttest_author1,test_author2.\n"
                                                            +"ADDRESS\tena,ebi,embl,UK\n"
                                                            + "TPA\ttrue\n"
                                                            + "FLATFILE\t" + flatFile.getFileName() + "\n" );

        WebinCliParameters parameters = AssemblyTestUtils.createWebinCliParameters(manifestFile, inputDir);

        TranscriptomeAssemblyWebinCli cli = new TranscriptomeAssemblyWebinCli();

        cli.setMetadataServiceActive(false);
        Sample sample = new Sample();
        sample.setBiosampleId("test_sample");
        cli.setSample(sample);

        Study study = new Study();
        study.setProjectId("test_study");
        cli.setStudy(study);

        try {
            cli.readManifest(parameters);
        } finally {
            SubmissionBundle sb = WebinCliTestUtils.prepareSubmissionBundle(cli);

            String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle(sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

            WebinCliTestUtils.assertAnalysisXml(analysisXml,
                    "<ANALYSIS_SET>\n" +
                            "<ANALYSIS>\n" +
                            "<TITLE>Transcriptome assembly: test_transcriptome</TITLE>\n" +
                            "<DESCRIPTION>" + descr + "</DESCRIPTION>\n" +
                            "<STUDY_REF accession=\"test_study\"/>\n" +
                            "<SAMPLE_REF accession=\"test_sample\"/>\n" +
                            "<ANALYSIS_TYPE>\n" +
                            "<TRANSCRIPTOME_ASSEMBLY>\n" +
                            "<NAME>test_transcriptome</NAME>\n" +
                            "<PROGRAM>test_program</PROGRAM>\n" +
                            "<PLATFORM>test_platform</PLATFORM>\n" +
                            "<AUTHORS>test_author1,test_author2.</AUTHORS>\n"+
                            "<ADDRESS>ena,ebi,embl,UK</ADDRESS>\n"+
                            "<TPA>true</TPA>\n" +
                            "</TRANSCRIPTOME_ASSEMBLY>\n" +
                            "</ANALYSIS_TYPE>\n" +
                            "<FILES>\n" +
                            "      <FILE filename=\"webin-cli/transcriptome/" + name + "/" + flatFile.getFileName() + "\" filetype=\"flatfile\" checksum_method=\"MD5\" checksum=\"e334ca8a758084ba2f9f5975e798039e\" />\n" +
                            "</FILES>\n" +
                            "</ANALYSIS>\n" +
                            "</ANALYSIS_SET>");
        }
    }
}
