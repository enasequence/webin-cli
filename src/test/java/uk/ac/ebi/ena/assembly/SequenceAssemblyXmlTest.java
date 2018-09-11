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
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

public class
SequenceAssemblyXmlTest
{
    @Before
    public void
    before()
    {
        Locale.setDefault( Locale.UK );
    }

    @Test
    public void
    testAnalysisXML_AssemblyInfo_WithFlatFile()
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
    testAnalysisXML_Manifest_WithFlatFile()
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
}
