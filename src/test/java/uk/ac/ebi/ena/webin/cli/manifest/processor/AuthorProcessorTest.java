package uk.ac.ebi.ena.webin.cli.manifest.processor;

import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.assembly.GenomeAssemblyManifest;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldDefinition;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;

import static org.junit.Assert.*;

public class AuthorProcessorTest {

    @Test
    public void testProcess() {
        ManifestFieldDefinition fieldDef = new ManifestFieldDefinition.Builder().
                type(ManifestFieldType.META).
                name(GenomeAssemblyManifest.Field.AUTHORS).
                desc(GenomeAssemblyManifest.Description.AUTHORS).optional().notInSpreadsheet().
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