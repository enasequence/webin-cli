package uk.ac.ebi.ena.webin.cli.message;

import java.text.MessageFormat;

public interface ValidationMessageSource {
    String text();

    default String format(Object... arguments) {
        return MessageFormat.format(text(), arguments);
    }

    default String regex() {
        return text().replaceAll("\\{[^\\{\\}]*\\}", ".*");
    }
}
