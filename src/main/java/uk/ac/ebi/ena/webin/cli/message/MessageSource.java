package uk.ac.ebi.ena.webin.cli.message;

public interface MessageSource {
    /** Message text with optional argument placeholders.
     */
    String text();

    /** Message text with argument placeholders replaced by argument strings.
     */
    String format(Object... arguments);

    /** Regular expression that can be used to match the message text
     * ignoring arguments and argument placeholders.
     */
    String regex();
}
