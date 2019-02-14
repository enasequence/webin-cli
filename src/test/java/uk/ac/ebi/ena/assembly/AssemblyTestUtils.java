package uk.ac.ebi.ena.assembly;

import uk.ac.ebi.embl.api.entry.feature.FeatureFactory;
import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.ena.WebinCliTestUtils;
import uk.ac.ebi.ena.entity.Sample;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

import java.io.File;
import java.nio.file.Path;

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
