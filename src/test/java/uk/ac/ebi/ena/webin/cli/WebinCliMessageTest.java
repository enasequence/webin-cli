package uk.ac.ebi.ena.webin.cli;

import org.junit.Test;
import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationMessage;

import static org.assertj.core.api.Assertions.assertThat;

public class WebinCliMessageTest {

    @Test
    public void testFormat() {
        assertThat(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.format()).isEqualTo(
                WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text);

        assertThat(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.format("TEST")).isEqualTo(
                WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text.replace("{0}", "TEST"));
    }

    @Test
    public void testError() {
        ValidationMessage<Origin> validationMessage = WebinCliMessage.error(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR);
        assertThat(validationMessage.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(validationMessage.getOrigins()).hasSize(0);
        assertThat(validationMessage.getMessage()).isEqualTo(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text);

        // Writer writer = new StringWriter();
        // validationMessage.writeMessage(writer);
        // assertThat(writer.toString()).isEqualTo("ERROR: " + WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text + "\n");

        validationMessage = WebinCliMessage.error(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR, () -> "TEST");
        assertThat(validationMessage.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(validationMessage.getOrigins()).hasSize(1);
        assertThat(validationMessage.getOrigins().get(0).getOriginText()).isEqualTo("TEST");
        assertThat(validationMessage.getMessage()).isEqualTo(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text);

        // writer = new StringWriter();
        // validationMessage.writeMessage(writer);
        // assertThat(writer.toString()).isEqualTo("ERROR: " + WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text + " [TEST]\n");
    }

    @Test
    public void testInfo() {
        ValidationMessage<Origin> validationMessage = WebinCliMessage.info(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR);
        assertThat(validationMessage.getSeverity()).isEqualTo(Severity.INFO);
        assertThat(validationMessage.getOrigins()).hasSize(0);
        assertThat(validationMessage.getMessage()).isEqualTo(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text);

        // Writer writer = new StringWriter();
        // validationMessage.writeMessage(writer);
        // assertThat(writer.toString()).isEqualTo("INFO: " + WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text + "\n");

        validationMessage = WebinCliMessage.info(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR, () -> "TEST");
        assertThat(validationMessage.getSeverity()).isEqualTo(Severity.INFO);
        assertThat(validationMessage.getOrigins()).hasSize(1);
        assertThat(validationMessage.getOrigins().get(0).getOriginText()).isEqualTo("TEST");
        assertThat(validationMessage.getMessage()).isEqualTo(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text);

        // writer = new StringWriter();
        // validationMessage.writeMessage(writer);
        // assertThat(writer.toString()).isEqualTo("INFO: " + WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text + " [TEST]\n");
    }
}

