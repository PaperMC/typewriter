package io.papermc.typewriter.parser;

public class ProtoTypeName {

    public static final char IDENTIFIER_SEPARATOR = '.';

    private final StringBuilder currentName;
    private char lastChar;
    private boolean idTerminatorExpected;

    public ProtoTypeName(char[] initialChars) {
        this.currentName = new StringBuilder(initialChars.length);
        this.append(initialChars);
    }

    public boolean append(char... namePart) {
        if (this.idTerminatorExpected) {
            if (namePart[0] != IDENTIFIER_SEPARATOR) {
                return false;
            } else {
                this.idTerminatorExpected = false;
            }
        }

        this.currentName.append(namePart);
        this.lastChar = namePart[namePart.length - 1];
        return true;
    }

    public void expectIdTerminator() {
        this.idTerminatorExpected = true;
    }

    public char getLastChar() {
        return this.lastChar;
    }

    public String getFinalName() {
        return this.currentName.toString();
    }

    public boolean shouldCheckStartIdentifier() {
        return this.lastChar == IDENTIFIER_SEPARATOR;
    }

}
