package uk.ac.ebi.ena.webin.cli.message;

public class ValidationOrigin {

    private final String key;
    private final String value;

    public ValidationOrigin(String key, Object value) {
        this.key = key;
        this.value = value.toString();
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        String str = getKey();
        if (getValue() != null && !getValue().isEmpty()) {
            str +=  ": " + getValue();
        }
        return str;
    }
}
