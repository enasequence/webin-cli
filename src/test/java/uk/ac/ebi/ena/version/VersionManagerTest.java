package uk.ac.ebi.ena.version;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import javax.script.ScriptException;

import org.junit.Assert;
import org.junit.Test;


public class 
VersionManagerTest 
{
    @Test public void
    test() throws IOException, InterruptedException, ScriptException, URISyntaxException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        VersionManager vm = new VersionManager();
        Assert.assertEquals( 0, vm.launchLatest( new String[] { "-version" } ) );
    }
}