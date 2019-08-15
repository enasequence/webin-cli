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
        assertThat(factory.createSampleProcessor()).isNotNull();
        assertThat(factory.createStudyProcessor()).isNotNull();
        assertThat(factory.createSampleXmlProcessor()).isNotNull();
        assertThat(factory.createAnalysisProcessor()).isNotNull();
        assertThat(factory.createRunProcessor()).isNotNull();
    }

    @Test
    public void
    testInactive() {
        MetadataProcessorParameters parameters = Mockito.mock(MetadataProcessorParameters.class);
        Mockito.when(parameters.isMetadataProcessorsActive()).thenReturn(false);
        MetadataProcessorFactory factory = new MetadataProcessorFactory(parameters);
        assertThat(factory.createSampleProcessor()).isNull();
        assertThat(factory.createStudyProcessor()).isNull();
        assertThat(factory.createSampleXmlProcessor()).isNull();
        assertThat(factory.createAnalysisProcessor()).isNull();
        assertThat(factory.createRunProcessor()).isNull();
    }

    @Test
    public void
    testNull() {
        MetadataProcessorParameters parameters = null;
        MetadataProcessorFactory factory = new MetadataProcessorFactory(parameters);
        assertThat(factory.createSampleProcessor()).isNull();
        assertThat(factory.createStudyProcessor()).isNull();
        assertThat(factory.createSampleXmlProcessor()).isNull();
        assertThat(factory.createAnalysisProcessor()).isNull();
        assertThat(factory.createRunProcessor()).isNull();
    }
}
