package uk.ac.ebi.ena.webin.cli.message.listener;

import uk.ac.ebi.ena.webin.cli.message.ValidationMessage;
import uk.ac.ebi.ena.webin.cli.message.ValidationMessage.Severity;

import java.util.function.Predicate;

public class MessageCounter implements MessageListener {

    private final Severity severity;
    private final Predicate<String> messagePredicate;
    private int count = 0;

    public MessageCounter(Severity severity, Predicate<String> messagePredicate) {
        this.severity = severity;
        this.messagePredicate = messagePredicate;
    }

    @Override
    public void listen(ValidationMessage message) {
        if (severity.equals(message.getSeverity()) &&
            messagePredicate.test(message.getMessage())) {
            count++;
        }
    }

    public int getCount() {
        return count;
    }

    public static MessageCounter text(Severity severity, String text) {
        return new MessageCounter(severity, m -> m.equals(text));
    }

    public static MessageCounter regex(Severity severity, String regex) {
        return new MessageCounter(severity, m -> m.matches(regex));
    }
}
