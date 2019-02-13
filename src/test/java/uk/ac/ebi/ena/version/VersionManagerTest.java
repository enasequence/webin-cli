package uk.ac.ebi.ena.version;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import javax.script.ScriptException;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.WebinCli;


public class 
VersionManagerTest 
{
    @Test public void
    test() throws IOException, ScriptException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        VersionManager vm = new VersionManager();
        Assert.assertEquals( WebinCli.SUCCESS, vm.launchLatest( new String[] { "-version" } ) );
        Assert.assertEquals( WebinCli.USER_ERROR, vm.launchLatest( new String[] { "-versin" } ) );
    }
}