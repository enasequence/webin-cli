package uk.ac.ebi.ena.webin.cli.message.listener;

import uk.ac.ebi.ena.webin.cli.message.ValidationMessage;

public interface MessageListener {

    void listen(ValidationMessage message);
}
