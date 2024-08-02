package io.papermc.typewriter.parser.name;

public class ProtoTypeName {

    public static final char IDENTIFIER_SEPARATOR = '.';

    protected StringBuilder name;

    public void append(String identifier) {
        if (this.name == null) {
            this.name = new StringBuilder();
        }
        this.name.append(identifier);
    }

    public void appendSeparator() {
        this.name.append(IDENTIFIER_SEPARATOR);
    }

    public void popSeparator() {
        this.name.deleteCharAt(this.name.length() - 1);
    }

    public String getTypeName() {
        return this.name.toString();
    }

    public boolean isEmpty() {
        return this.name == null || this.name.isEmpty();
    }
}
