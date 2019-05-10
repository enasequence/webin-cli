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
import uk.ac.ebi.ena.webin.cli.entity.Study;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;

public class
SequenceAssemblyXmlTest
{
    @Before public void
    before()
    {
        Locale.setDefault( Locale.UK );
    }

    
    @Test public void
    testAnalysisXML_AssemblyInfo_WithFlatFile()
    {
        Path flatFile = WebinCliTestUtils.createGzippedTempFile( "flatfile.dat.gz", "ID   ;" );
       	SubmissionOptions submissionOptions =  new SubmissionOptions();
       	SubmissionFiles submissionFiles = new SubmissionFiles();
       	SubmissionFile submissionFile = new SubmissionFile( FileType.FLATFILE,flatFile.toFile() );
       	submissionFiles.addFile( submissionFile );
       	submissionOptions.submissionFiles = Optional.of( submissionFiles );
        SequenceAssemblyWebinCli cli = new SequenceAssemblyWebinCli();
        String name = "test_sequence";
        cli.setName( name );
        cli.setSubmissionOptions( submissionOptions );
        cli.getParameters().setInputDir( flatFile.getParent().toFile() );

        AssemblyInfoEntry info = new AssemblyInfoEntry();
        info.setName( name );
        info.setStudyId( "test_study" );
        cli.setAssemblyInfo( info );
        cli.setDescription( "sequence description" );

        cli.setAnalysisRef( new ArrayList<Analysis>() { { add( new Analysis( "ANALYSIS_ID1", "ANALYSIS_ID1_ALIAS" ) ); add( new Analysis( "ANALYSIS_ID2", "ANALYSIS_ID2_ALIAS" ) ); } } );
        cli.setRunRef( new ArrayList<Run>() { { add( new Run( "RUN_ID1", "RUN_ID1_ALIAS" ) ); add( new Run( "RUN_ID2", "RUN_ID2_ALIAS" ) ); } } );

        SubmissionBundle sb = WebinCliTestUtils.prepareSubmissionBundle( cli );

        String analysisXml = WebinCliTestUtils.readXmlFromSubmissionBundle( sb, SubmissionBundle.SubmissionXMLFileType.ANALYSIS );

        WebinCliTestUtils.assertAnalysisXml( analysisXml,
                "<ANALYSIS_SET>\n"
                      + "<ANALYSIS>\n"
                      + "<TITLE>Sequence assembly: test_sequence</TITLE>\n"
                      + "<DESCRIPTION>" + cli.getDescription() + "</DESCRIPTION>\n"
                      + "<STUDY_REF accession=\"test_study\"/>\n"
                      + "    <RUN_REF accession=\"RUN_ID1\"/>\n"
                      + "    <RUN_REF accession=\"RUN_ID2\"/>\n"
                      + "    <ANALYSIS_REF accession=\"ANALYSIS_ID1\"/>\n"
                      + "    <ANALYSIS_REF accession=\"ANALYSIS_ID2\"/>\n"
                      + "<ANALYSIS_TYPE>\n"
                      + "<SEQUENCE_FLATFILE/>\n"
                      + "</ANALYSIS_TYPE>\n"
                      + "<FILES>\n"
                      + "      <FILE filename=\"webin-cli/sequence/" + name + "/" + flatFile.getFileName() + "\" filetype=\"flatfile\" checksum_method=\"MD5\" checksum=\"e334ca8a758084ba2f9f5975e798039e\" />\n"
                      + "</FILES>\n"
                      + "</ANALYSIS>\n"
                      + "</ANALYSIS_SET>" );
    }

    
    @Test public void
    testAnalysisXML_Manifest_WithFlatFile()
    {
        String name = "test_sequence";
        Path flatFile = WebinCliTestUtils.createGzippedTempFile("flatfile.dat.gz", "ID   ;");
        Path inputDir = flatFile.getParent();
        Path manifestFile = WebinCliTestUtils.createTempFile("manifest.txt", inputDir,
                "NAME\t" + name + "\n" +
                        "STUDY\ttest_study\n" +
                        "FLATFILE\t" + flatFile.getFileName() + "\n" +
                        SequenceAssemblyManifest.Field.DESCRIPTION + " d e s c r i p t i o n"
        );

        WebinCliParameters parameters = AssemblyTestUtils.createWebinCliParameters(manifestFile, inputDir);

        SequenceAssemblyWebinCli cli = new SequenceAssemblyWebinCli();

        cli.setMetadataServiceActive(false);
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
                            "<ANALYSIS>\n" +
                            "<TITLE>Sequence assembly: test_sequence</TITLE>\n" +
                            "<DESCRIPTION>" + cli.getDescription() + "</DESCRIPTION>\n" +
                            "<STUDY_REF accession=\"test_study\"/>\n" +
                            "<ANALYSIS_TYPE>\n" +
                            "<SEQUENCE_FLATFILE/>\n" +
                            "</ANALYSIS_TYPE>\n" +
                            "<FILES>\n" +
                            "      <FILE filename=\"webin-cli/sequence/" + name + "/" + flatFile.getFileName() + "\" filetype=\"flatfile\" checksum_method=\"MD5\" checksum=\"e334ca8a758084ba2f9f5975e798039e\" />\n" +
                            "</FILES>\n" +
                            "</ANALYSIS>\n" +
                            "</ANALYSIS_SET>");
        }
    }
}
