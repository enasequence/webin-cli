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
package uk.ac.ebi.ena.webin.cli.context.genome;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.ena.webin.cli.context.genome.GenomeManifestReader.ASSEMBLY_TYPE_PRIMARY_METAGENOME;
import static uk.ac.ebi.ena.webin.cli.context.genome.GenomeManifestReader.Field;

import java.nio.file.Paths;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.TempFileBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest;

public class
GenomeManifestReaderTest {
    private static GenomeManifestReader createManifestReader() {
        WebinCliParameters parameters = WebinCliTestUtils.getTestWebinCliParameters();
        return new GenomeManifestReader(parameters, new MetadataProcessorFactory(parameters));
    }

    @Before
    public void
    before() {
        Locale.setDefault(Locale.UK);
    }

    @Test
    public void
    testValidManifestWithoutAssemblyType() {
        GenomeManifestReader manifestReader = createManifestReader();
        GenomeManifest manifest = manifestReader.getManifest();

        Assert.assertNull(manifest.getStudy());
        Assert.assertNull(manifest.getSample());
        Assert.assertNull(manifest.getPlatform());
        Assert.assertNull(manifest.getName());
        assertThat(manifest.files().files()).size().isZero();
        Assert.assertNull(manifest.getDescription());
        Assert.assertNull(manifest.getSubmissionTool());
        Assert.assertNull(manifest.getSubmissionToolVersion());

        manifestReader.readManifest(Paths.get("."),
                new ManifestBuilder()
                        .field(Field.PLATFORM, " illumina")
                        .field(Field.NAME, " SOME-FANCY-NAME")
                        .field(Field.DESCRIPTION, " description")
                        .file("FASTA", TempFileBuilder.empty("fasta"))
                        .field(ManifestReader.Fields.SUBMISSION_TOOL, "ST-001")
                        .field(ManifestReader.Fields.SUBMISSION_TOOL_VERSION, "STV-001")
                        .build());

        Assert.assertEquals("illumina", manifest.getPlatform());
        Assert.assertEquals("SOME-FANCY-NAME", manifest.getName());
        assertThat(manifest.files().files()).size().isOne();
        Assert.assertEquals("description", manifest.getDescription());
        Assert.assertEquals("ST-001", manifest.getSubmissionTool());
        Assert.assertEquals("STV-001", manifest.getSubmissionToolVersion());
        Assert.assertNull(manifest.getAssemblyType());
    }

    @Test
    public void
    testValidManifestWithAssemblyType() {
        GenomeManifestReader manifestReader = createManifestReader();
        GenomeManifest manifest = manifestReader.getManifest();

        Assert.assertNull(manifest.getStudy());
        Assert.assertNull(manifest.getSample());
        Assert.assertNull(manifest.getPlatform());
        Assert.assertNull(manifest.getName());
        assertThat(manifest.files().files()).size().isZero();
        Assert.assertNull(manifest.getDescription());
        Assert.assertNull(manifest.getSubmissionTool());
        Assert.assertNull(manifest.getSubmissionToolVersion());

        manifestReader.readManifest(Paths.get("."),
                new ManifestBuilder()
                        .field(Field.PLATFORM, " illumina")
                        .field(Field.NAME, " SOME-FANCY-NAME")
                        .field(Field.DESCRIPTION, " description")
                        .field(Field.ASSEMBLY_TYPE, ASSEMBLY_TYPE_PRIMARY_METAGENOME)
                        .file("FASTA", TempFileBuilder.empty("fasta"))
                        .field(ManifestReader.Fields.SUBMISSION_TOOL, "ST-001")
                        .field(ManifestReader.Fields.SUBMISSION_TOOL_VERSION, "STV-001")
                        .build());

        Assert.assertEquals("illumina", manifest.getPlatform());
        Assert.assertEquals("SOME-FANCY-NAME", manifest.getName());
        assertThat(manifest.files().files()).size().isOne();
        Assert.assertEquals("description", manifest.getDescription());
        Assert.assertEquals("ST-001", manifest.getSubmissionTool());
        Assert.assertEquals("STV-001", manifest.getSubmissionToolVersion());
        Assert.assertEquals(ASSEMBLY_TYPE_PRIMARY_METAGENOME, manifest.getAssemblyType());
    }
}
