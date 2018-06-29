package uk.ac.ebi.ena.transcriptome;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.ena.assembly.TranscriptomeAssemblyWebinCli;
import uk.ac.ebi.ena.manifest.ManifestFileReader;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.study.Study;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;

public class TranscriptomeAssemblyWebinCliTest {

    @Before
    public void
    before()
    {
        Locale.setDefault( Locale.UK );
    }

    private File
    createOutputFolder() throws IOException
    {
        File output = File.createTempFile( "test", "test" );
        Assert.assertTrue( output.delete() );
        Assert.assertTrue( output.mkdirs() );
        return output;
    }

    private ManifestFileReader getManifestFileReader(String manifestFileName) {
        URL url = TranscriptomeAssemblyWebinCliTest.class.getClassLoader().getResource(
                "uk/ac/ebi/ena/transcriptome/" + manifestFileName);
        File file = new File(url.getFile());
        ManifestFileReader reader = new ManifestFileReader();
        try {
            reader.read(file.getPath());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return reader;
    }

    @Test
    public void
    testSimpleFastaSuccess() throws Exception
    {
        Sample sample = new Sample();
        sample.setOrganism( "Quercus robur" );
        Study study = new Study();

        TranscriptomeAssemblyWebinCli validator = new TranscriptomeAssemblyWebinCli(
                getManifestFileReader("simple_fasta/transcriptome.manifest"), sample, study);
        validator.setValidationDir( createOutputFolder() );
        validator.setSubmitDir( createOutputFolder() );
        validator.setName( "test" );
        Assert.assertTrue( validator.validate() );
    }

    @Test
    public void
    testSimpleFastaMissingSampleOrganismError() throws Exception
    {
        Sample sample = new Sample();
        sample.setOrganism( null );
        Study study = new Study();

        TranscriptomeAssemblyWebinCli validator = new TranscriptomeAssemblyWebinCli(
                getManifestFileReader("simple_fasta/transcriptome.manifest"), sample, study);
        validator.setValidationDir( createOutputFolder() );
        validator.setSubmitDir( createOutputFolder() );
        validator.setName( "test" );
        Assert.assertFalse( validator.validate() );
    }
}
