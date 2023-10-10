/*
 * Copyright 2018-2023 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.submit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle.SubmissionXMLFile;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;

public class SubmissionBundleTest {

    @Test
    public void test() throws IOException {
        File submitDirectory = Files.createTempDirectory( "TEST-SUBMITION-BUNDLE" ).toFile();
        String uploadDirectory = Files.createTempDirectory( "TEST-SUBMITION-BUNDLE" ).toString();

        SubmissionBundle.SubmissionXMLFile xmlFile = new SubmissionXMLFile(
            SubmissionBundle.SubmissionXMLFileType.SUBMISSION,
            new File(submitDirectory, "submission.xml"),
            "<SUBMISSION_SET><SUBMISSION><ACTIONS><ACTION><ADD/></ACTION></ACTIONS></SUBMISSION></SUBMISSION_SET>");
        xmlFile.setMd5(FileUtils.calculateDigest( "MD5", xmlFile.getXmlContent().getBytes(StandardCharsets.UTF_8) ));

        List<SubmissionXMLFile> xmlFileList = Arrays.asList(xmlFile);

        File manifestFile = File.createTempFile( "TEST-SB", "MANIFEST" );
        String manifestMd5 = FileUtils.calculateDigest( "MD5", manifestFile );

        List<File> uploadFileList = Arrays.asList( manifestFile );

        SubmissionBundle expectedSb = new SubmissionBundle( submitDirectory, uploadDirectory, uploadFileList, xmlFileList, manifestMd5 );

        SubmissionBundleHelper.write( expectedSb, submitDirectory );
        
        SubmissionBundle actualSb = SubmissionBundleHelper.read( manifestMd5, submitDirectory );

        Assert.assertEquals( expectedSb, actualSb );
        Assert.assertEquals( expectedSb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.SUBMISSION).getXmlContent(),
            actualSb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.SUBMISSION).getXmlContent() );
    }
}
