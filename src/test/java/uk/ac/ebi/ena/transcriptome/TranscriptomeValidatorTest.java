/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.transcriptome;

public class TranscriptomeValidatorTest {
    private final static String TRANSCRIPTOME_BASE_DIR = "src/test/resources/uk/ac/ebi/ena/transcriptome/tsvfile";
    /*
    @Test
    public void testValidFastaFile() throws Exception {
        String fileName=null;
        URL url = TranscriptomeValidatorTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/transcriptome/manifestwithinfowithmissingrequireedfields.txt");
        if (url != null)
            fileName = url.getPath().replaceAll("%20", " ");
        ManifestFileReader reader= new ManifestFileReader();
        reader.read(fileName);
        AssemblyInfoEntry assemblyInfoEntry = new AssemblyInfoEntry();
        assemblyInfoEntry.setMoleculeType("genomic DNA");
        List<String> locusTagsList = new ArrayList<>();
        locusTagsList.add("MyLosusTag");
        WebinCliInterface validator = new TranscriptomeAssemblyWebinCli(reader, assemblyInfoEntry, "Homo Sapiens", locusTagsList);
        int i= validator.validate();
        assertEquals(2, i);
    }
    */
}
