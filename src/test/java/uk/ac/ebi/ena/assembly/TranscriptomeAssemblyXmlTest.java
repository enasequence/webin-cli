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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.ena.WebinCliTestUtils;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

public class
TranscriptomeAssemblyXmlTest {
    @Before
    public void
    before() {
        Locale.setDefault(Locale.UK);
    }

    @Test
    public void
    testAnalysisXML_AssemblyInfo_WithFastaFile() {
        TranscriptomeAssemblyWebinCli cli = new TranscriptomeAssemblyWebinCli();
        String name = "test_transcriptome";
        cli.setName(name);

        Path fastaFile = WebinCliTestUtils.createTempFile(false, ">123\nACGT");
        cli.getParameters().setInputDir(fastaFile.getParent().toFile());
        cli.fastaFiles = Arrays.asList(new File(fastaFile.toString()));

        AssemblyInfoEntry info = new AssemblyInfoEntry();
        cli.setAssemblyInfo(info);
        info.setName(name);
        info.setSampleId("test_sample");
        info.setStudyId("test_study");
        info.setProgram("test_program");
        info.setPlatform("test_platform");
        info.setTpa(false);

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

    @Test
    public void
    testAnalysisXML_Manifest_WithFlatFile() {
        String name = "test_transcriptome";
        Path flatFile = WebinCliTestUtils.createTempFile("flatfile.dat.gz", true, "ID   ;");
        Path inputDir = flatFile.getParent();
        Path manifestFile = WebinCliTestUtils.createTempFile("manifest.txt", inputDir, false,
                "NAME\t" + name + "\n" +
                        "SAMPLE\ttest_sample\n" +
                        "STUDY\ttest_study\n" +
                        "PROGRAM\ttest_program\n" +
                        "PLATFORM\ttest_platform\n" +
                        "TPA\ttrue\n" +
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

        try {
            cli.init(parameters);
        } finally {
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
                            "<TPA>true</TPA>\n" +
                            "</TRANSCRIPTOME_ASSEMBLY>\n" +
                            "</ANALYSIS_TYPE>\n" +
                            "<FILES>\n" +
                            "      <FILE filename=\"transcriptome/" + name + "/" + flatFile.getFileName() + "\" filetype=\"flatfile\" checksum_method=\"MD5\" checksum=\"e334ca8a758084ba2f9f5975e798039e\" />\n" +
                            "</FILES>\n" +
                            "</ANALYSIS>\n" +
                            "</ANALYSIS_SET>");
        }
    }
}

