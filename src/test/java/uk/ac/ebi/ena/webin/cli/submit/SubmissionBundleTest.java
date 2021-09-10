/*
 * Copyright 2018-2021 EMBL - European Bioinformatics Institute
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
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle.SubmissionXMLFile;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;

public class 
SubmissionBundleTest 
{

    @Test public void
    test() throws IOException
    {
        File submitDirectory = Files.createTempDirectory( "TEST-SUBMITION-BUNDLE" ).toFile();
        String uploadDirectory = Files.createTempDirectory( "TEST-SUBMITION-BUNDLE" ).toString();
        
        File manifestFile = File.createTempFile( "TEST-SB", "MANIFEST" );
        List<File> uploadFileList = Arrays.asList( manifestFile );
        List<SubmissionXMLFile> xmlFileList = Collections.emptyList();
        
        String center_name = "Some center name";
        String md5 = FileUtils.calculateDigest( "MD5", manifestFile );
        
        SubmissionBundle sb = new SubmissionBundle( submitDirectory, uploadDirectory, uploadFileList, xmlFileList, center_name, FileUtils.calculateDigest( "MD5", manifestFile ) );
        SubmissionBundleHelper serialiser = new SubmissionBundleHelper( Files.createTempFile( ".data", "MANIFEST" ).toString() );
        serialiser.write( sb );
        
        SubmissionBundle sb1 = serialiser.read( md5 );
        Assert.assertEquals( sb, sb1 );
    }
}
