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
package uk.ac.ebi.ena.webin.cli;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import uk.ac.ebi.ena.webin.cli.validator.reference.Sample;

public class WebinCliTestUtils {

    public static WebinCliParameters getTestWebinCliParameters() {
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setSubmissionAccount(getTestWebinUsername());
        parameters.setUsername(getTestWebinUsername());
        parameters.setPassword(getTestWebinPassword());
        parameters.setTest(true);
        return parameters;
    }

    public static String getTestWebinUsername() {
        String username = System.getenv("webin-cli-username");
        Assert.assertNotNull("please set up environment variable: webin-cli-username", username);
        Assert.assertTrue( "please use Webin-N in environment variable: webin-cli-username", username.startsWith("Webin-"));
        return username;
    }

    public static String getTestWebinPassword() {
        String password = System.getenv("webin-cli-password");
        Assert.assertNotNull("please set up environment variable: webin-cli-password", password);
        return password;
    }

    public static File getResourceDir(String dir) {
        try {
            ResourcePatternResolver resolver =
                    new PathMatchingResourcePatternResolver(WebinCliTestUtils.class.getClassLoader());
            Resource[] resources = resolver.getResources("classpath*:" + dir + "/*");
            for (Resource resource : resources) {
                if (resource.getFile().getAbsolutePath().contains("resources")) {
                    return resource.getFile().getParentFile();
                }
            }
            return null;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Sample getDefaultSample() {
        Sample sample = new Sample();
        sample.setOrganism("Quercus robur");
        return sample;
    }

    public static File createTempDir() {
        try {
            File folder = File.createTempFile("test", "test");
            Assert.assertTrue(folder.delete());
            Assert.assertTrue(folder.mkdirs());
            return folder;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
