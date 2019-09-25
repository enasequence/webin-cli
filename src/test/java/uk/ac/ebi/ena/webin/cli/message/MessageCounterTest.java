package uk.ac.ebi.ena.webin.cli.message;

import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.message.listener.MessageCounter;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageCounterTest {

    @Test
    public void testError() {
        ValidationResult result = new ValidationResult();
        MessageCounter textCounter = MessageCounter.text(ValidationMessage.Severity.ERROR, "TEST");
        MessageCounter regexCounter = MessageCounter.regex(ValidationMessage.Severity.ERROR, "TEST.*");
        result.add(textCounter);
        result.add(regexCounter);

        assertThat(textCounter.getCount()).isZero();
        assertThat(regexCounter.getCount()).isZero();

        result.add(new ValidationMessage(ValidationMessage.Severity.INFO, "TEST"));
        assertThat(textCounter.getCount()).isZero();
        assertThat(regexCounter.getCount()).isZero();

        result.add(new ValidationMessage(ValidationMessage.Severity.ERROR, "TEST"));
        assertThat(textCounter.getCount()).isOne();
        assertThat(regexCounter.getCount()).isOne();

        result.add(new ValidationMessage(ValidationMessage.Severity.ERROR, "TEST"));
        assertThat(textCounter.getCount()).isEqualTo(2);
        assertThat(regexCounter.getCount()).isEqualTo(2);

        result.add(new ValidationMessage(ValidationMessage.Severity.ERROR, "TEST1"));
        assertThat(textCounter.getCount()).isEqualTo(2);
        assertThat(regexCounter.getCount()).isEqualTo(3);

        result.add(new ValidationMessage(ValidationMessage.Severity.ERROR, "TEST2"));
        assertThat(textCounter.getCount()).isEqualTo(2);
        assertThat(regexCounter.getCount()).isEqualTo(4);
    }

    @Test
    public void testInfo() {
        ValidationResult result = new ValidationResult();
        MessageCounter textCounter = MessageCounter.text(ValidationMessage.Severity.INFO, "TEST");
        MessageCounter regexCounter = MessageCounter.regex(ValidationMessage.Severity.INFO, "TEST.*");
        result.add(textCounter);
        result.add(regexCounter);

        assertThat(textCounter.getCount()).isZero();
        assertThat(regexCounter.getCount()).isZero();

        result.add(new ValidationMessage(ValidationMessage.Severity.ERROR, "TEST"));
        assertThat(textCounter.getCount()).isZero();
        assertThat(regexCounter.getCount()).isZero();

        result.add(new ValidationMessage(ValidationMessage.Severity.INFO, "TEST"));
        assertThat(textCounter.getCount()).isOne();
        assertThat(regexCounter.getCount()).isOne();

        result.add(new ValidationMessage(ValidationMessage.Severity.INFO, "TEST"));
        assertThat(textCounter.getCount()).isEqualTo(2);
        assertThat(regexCounter.getCount()).isEqualTo(2);

        result.add(new ValidationMessage(ValidationMessage.Severity.INFO, "TEST1"));
        assertThat(textCounter.getCount()).isEqualTo(2);
        assertThat(regexCounter.getCount()).isEqualTo(3);

        result.add(new ValidationMessage(ValidationMessage.Severity.INFO, "TEST2"));
        assertThat(textCounter.getCount()).isEqualTo(2);
        assertThat(regexCounter.getCount()).isEqualTo(4);
    }

}
