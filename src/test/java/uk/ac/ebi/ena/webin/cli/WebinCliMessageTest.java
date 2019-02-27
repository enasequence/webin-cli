package uk.ac.ebi.ena.webin.cli;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WebinCliMessageTest {

    @Test
    public void test() {
        assertThat(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.format()).isEqualTo(
                WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text);

        assertThat(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.format("TEST")).isEqualTo(
                WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text.replace("{0}", "TEST"));

    }
}

