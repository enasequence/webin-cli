package uk.ac.ebi.ena.webin.cli.manifest.processor;

import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import static org.assertj.core.api.Assertions.assertThat;

public class MetadataProcessorFactoryTest {

    @Test
    public void
    testActive() {
        MetadataProcessorFactory factory = new MetadataProcessorFactory();
        assertThat(factory.createSampleProcessor(new WebinCliParameters())).isNotNull();
        assertThat(factory.createStudyProcessor(new WebinCliParameters())).isNotNull();
        assertThat(factory.createSampleXmlProcessor(new WebinCliParameters())).isNotNull();
        assertThat(factory.createAnalysisProcessor(new WebinCliParameters())).isNotNull();
        assertThat(factory.createRunProcessor(new WebinCliParameters())).isNotNull();
        factory.setActive(true);
        assertThat(factory.createSampleProcessor(new WebinCliParameters())).isNotNull();
        assertThat(factory.createStudyProcessor(new WebinCliParameters())).isNotNull();
        assertThat(factory.createSampleXmlProcessor(new WebinCliParameters())).isNotNull();
        assertThat(factory.createAnalysisProcessor(new WebinCliParameters())).isNotNull();
        assertThat(factory.createRunProcessor(new WebinCliParameters())).isNotNull();
    }

    @Test
    public void
    testInactive() {
        MetadataProcessorFactory factory = new MetadataProcessorFactory();
        factory.setActive(false);
        assertThat(factory.createSampleProcessor(new WebinCliParameters())).isNull();
        assertThat(factory.createStudyProcessor(new WebinCliParameters())).isNull();
        assertThat(factory.createSampleXmlProcessor(new WebinCliParameters())).isNull();
        assertThat(factory.createAnalysisProcessor(new WebinCliParameters())).isNull();
        assertThat(factory.createRunProcessor(new WebinCliParameters())).isNull();
    }
}
