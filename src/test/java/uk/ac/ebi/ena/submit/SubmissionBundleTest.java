package uk.ac.ebi.ena.submit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.ebi.ena.submit.SubmissionBundle.SubmissionXMLFile;
import uk.ac.ebi.ena.utils.FileUtils;

public class 
SubmissionBundleTest 
{

    @Test public void
    test() throws NoSuchAlgorithmException, IOException
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
