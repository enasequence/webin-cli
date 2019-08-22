package uk.ac.ebi.ena.webin.cli.manifest.processor;

import org.junit.Test;
import org.mockito.Mockito;
import static org.assertj.core.api.Assertions.assertThat;

public class MetadataProcessorFactoryTest {

    @Test
    public void
    testActive() {
        MetadataProcessorParameters parameters = Mockito.mock(MetadataProcessorParameters.class);
        Mockito.when(parameters.isMetadataProcessorsActive()).thenReturn(true);
        MetadataProcessorFactory factory = new MetadataProcessorFactory(parameters);
        assertThat(factory.getSampleProcessor()).isNotNull();
        assertThat(factory.getStudyProcessor()).isNotNull();
        assertThat(factory.getSampleXmlProcessor()).isNotNull();
        assertThat(factory.getAnalysisProcessor()).isNotNull();
        assertThat(factory.getRunProcessor()).isNotNull();
    }

    @Test
    public void
    testInactive() {
        MetadataProcessorParameters parameters = Mockito.mock(MetadataProcessorParameters.class);
        Mockito.when(parameters.isMetadataProcessorsActive()).thenReturn(false);
        MetadataProcessorFactory factory = new MetadataProcessorFactory(parameters);
        assertThat(factory.getSampleProcessor()).isNull();
        assertThat(factory.getStudyProcessor()).isNull();
        assertThat(factory.getSampleXmlProcessor()).isNull();
        assertThat(factory.getAnalysisProcessor()).isNull();
        assertThat(factory.getRunProcessor()).isNull();
    }

    @Test
    public void
    testNull() {
        MetadataProcessorParameters parameters = null;
        MetadataProcessorFactory factory = new MetadataProcessorFactory(parameters);
        assertThat(factory.getSampleProcessor()).isNull();
        assertThat(factory.getStudyProcessor()).isNull();
        assertThat(factory.getSampleXmlProcessor()).isNull();
        assertThat(factory.getAnalysisProcessor()).isNull();
        assertThat(factory.getRunProcessor()).isNull();
    }
}
