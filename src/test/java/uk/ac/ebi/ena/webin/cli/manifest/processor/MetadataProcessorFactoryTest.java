package uk.ac.ebi.ena.webin.cli.manifest.processor;

import org.junit.Test;
import org.mockito.Mockito;
import uk.ac.ebi.ena.webin.cli.manifest.processor.metadata.*;

import static org.assertj.core.api.Assertions.assertThat;

public class MetadataProcessorFactoryTest {

    @Test
    public void
    testDefaultProcessor() {
        MetadataProcessorParameters parameters = Mockito.mock(MetadataProcessorParameters.class);
        MetadataProcessorFactory factory = new MetadataProcessorFactory(parameters);
        assertThat(factory.getSampleProcessor()).isNotNull();
        assertThat(factory.getStudyProcessor()).isNotNull();
        assertThat(factory.getSampleXmlProcessor()).isNotNull();
        assertThat(factory.getAnalysisProcessor()).isNotNull();
        assertThat(factory.getRunProcessor()).isNotNull();
    }

    @Test
    public void
    testParameterProcessor() {
        MetadataProcessorParameters parameters = Mockito.mock(MetadataProcessorParameters.class);
        StudyProcessor studyProcessor = new StudyProcessor(parameters);
        SampleProcessor sampleProcessor = new SampleProcessor(parameters);
        SampleXmlProcessor sampleXmlProcessor = new SampleXmlProcessor(parameters);
        RunProcessor runProcessor = new RunProcessor(parameters);
        AnalysisProcessor analysisProcessor = new AnalysisProcessor(parameters);

        Mockito.when(parameters.getStudyProcessor()).thenReturn(studyProcessor);
        Mockito.when(parameters.getSampleProcessor()).thenReturn(sampleProcessor);
        Mockito.when(parameters.getSampleXmlProcessor()).thenReturn(sampleXmlProcessor);
        Mockito.when(parameters.getRunProcessor()).thenReturn(runProcessor);
        Mockito.when(parameters.getAnalysisProcessor()).thenReturn(analysisProcessor);

        MetadataProcessorFactory factory = new MetadataProcessorFactory(parameters);
        assertThat(factory.getSampleProcessor()).isSameAs(sampleProcessor);
        assertThat(factory.getStudyProcessor()).isSameAs(studyProcessor);
        assertThat(factory.getSampleXmlProcessor()).isSameAs(sampleXmlProcessor);
        assertThat(factory.getAnalysisProcessor()).isSameAs(analysisProcessor);
        assertThat(factory.getRunProcessor()).isSameAs(runProcessor);
    }

    @Test
    public void
    testNullParameters() {
        MetadataProcessorParameters parameters = null;
        MetadataProcessorFactory factory = new MetadataProcessorFactory(parameters);
        assertThat(factory.getSampleProcessor()).isNull();
        assertThat(factory.getStudyProcessor()).isNull();
        assertThat(factory.getSampleXmlProcessor()).isNull();
        assertThat(factory.getAnalysisProcessor()).isNull();
        assertThat(factory.getRunProcessor()).isNull();
    }
}
