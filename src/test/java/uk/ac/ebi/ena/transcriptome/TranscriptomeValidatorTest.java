package uk.ac.ebi.ena.transcriptome;

public class TranscriptomeValidatorTest {
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
