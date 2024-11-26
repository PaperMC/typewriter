package io.papermc.typewriter.parser.name;

public class ProtoQualifiedName {

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

    public String getName() {
        return this.name.toString();
    }
}
