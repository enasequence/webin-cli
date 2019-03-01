
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

import java.io.File;
import java.nio.file.Path;

import uk.ac.ebi.embl.api.entry.feature.FeatureFactory;
import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.entity.Sample;

class AssemblyTestUtils {

    static Sample getDefaultSample() {
        Sample sample = new Sample();
        sample.setOrganism("Quercus robur");
        return sample;
    }

    static SourceFeature getDefaultSourceFeature() {
        SourceFeature source = new FeatureFactory().createSourceFeature();
        source.setScientificName("Micrococcus sp. 5");
        return source;
    }

    static Sample getHumanSample() {
        Sample sample = new Sample();
        sample.setOrganism("Homo sapiens");
        return sample;
    }

    static SourceFeature getHumanSourceFeature() {
        SourceFeature source = new FeatureFactory().createSourceFeature();
        source.setScientificName("Homo sapiens");
        return source;
    }

    static WebinCliParameters
    createWebinCliParameters(Path manifestFile, Path inputDir ) {
        return createWebinCliParameters(manifestFile.toFile(), inputDir.toFile());
    }

    static WebinCliParameters
    createWebinCliParameters(File manifestFile, File inputDir ) {
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setManifestFile(manifestFile);
        parameters.setInputDir(inputDir);
        parameters.setOutputDir(WebinCliTestUtils.createTempDir());
        parameters.setUsername(System.getenv( "webin-cli-username" ));
        parameters.setPassword(System.getenv( "webin-cli-password" ));
        parameters.setTestMode(true);
        return parameters;
    }
}
