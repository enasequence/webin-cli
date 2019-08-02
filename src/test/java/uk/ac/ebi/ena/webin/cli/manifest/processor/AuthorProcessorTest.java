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
package uk.ac.ebi.ena.webin.cli.manifest.processor;

import static org.junit.Assert.*;

import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.assembly.GenomeAssemblyManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldDefinition;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;

public class AuthorProcessorTest {

    @Test
    public void testProcess() {
        ManifestFieldDefinition fieldDef = new ManifestFieldDefinition.Builder().
                type(ManifestFieldType.META).
                name(GenomeAssemblyManifestReader.Field.AUTHORS).
                desc(GenomeAssemblyManifestReader.Description.AUTHORS).optional().notInSpreadsheet().
                build().get(0);
        ManifestFieldValue manifestField = new ManifestFieldValue(fieldDef,"Senthil .V", null);
        new AuthorProcessor().process(manifestField);
        assertEquals("Senthil .V", manifestField.getValue());

        manifestField = new ManifestFieldValue(fieldDef,"Senthil .V,   nathan vijay.", null);
        new AuthorProcessor().process(manifestField);
        assertEquals("Senthil .V, nathan vijay.", manifestField.getValue());

        manifestField = new ManifestFieldValue(fieldDef,"Senthil .V...,nathan", null);
        new AuthorProcessor().process(manifestField);
        assertEquals("Senthil .V.,nathan", manifestField.getValue());

        manifestField = new ManifestFieldValue(fieldDef,"Senthil .V.", null);
        new AuthorProcessor().process(manifestField);
        assertEquals("Senthil .V.", manifestField.getValue());

        manifestField = new ManifestFieldValue(fieldDef,"..Senthil Vija.  vija; nathan", null);
        new AuthorProcessor().process(manifestField);
        assertEquals(".Senthil Vija. vija nathan", manifestField.getValue());

        manifestField = new ManifestFieldValue(fieldDef,"", null);
        new AuthorProcessor().process(manifestField);
        assertEquals( "",manifestField.getValue());
    }

}