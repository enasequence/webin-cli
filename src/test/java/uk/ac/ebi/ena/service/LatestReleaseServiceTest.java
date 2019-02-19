package uk.ac.ebi.ena.service;

import java.io.IOException;
import java.net.URL;

import javax.script.ScriptException;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.ebi.ena.service.LatestReleaseService.GitHubReleaseInfo;

public class 
LatestReleaseServiceTest 
{
    @Test public void
    test() throws IOException, ScriptException {
        LatestReleaseService lr = new LatestReleaseService.Builder().build();

        GitHubReleaseInfo info = lr.getLatestInfo();
        
        Assert.assertFalse( info.assets.isEmpty() );
        Assert.assertEquals( 1L, info.assets.size() );
        Assert.assertNotNull( new URL( info.assets.get( 0 ).browser_download_url ).toString() );
    }
}
