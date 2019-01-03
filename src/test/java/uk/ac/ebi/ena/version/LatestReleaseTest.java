package uk.ac.ebi.ena.version;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.script.ScriptException;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.ebi.ena.version.LatestRelease.GitHubReleaseInfo;

public class 
LatestReleaseTest 
{
    @Test public void
    test() throws MalformedURLException, IOException, ScriptException, URISyntaxException
    {
        LatestRelease lr = new LatestRelease();
        GitHubReleaseInfo info = lr.getLatestInfo();
        
        Assert.assertFalse( info.assets.isEmpty() );
        Assert.assertEquals( 1L, info.assets.size() );
        Assert.assertNotNull( new URL( info.assets.get( 0 ).browser_download_url ).toString() );
    }
}
