package io.papermc.typewriter.parser;

public class UnicodeTranslates {
    protected int cursor;
    private final char[] value;
    private int charSize = 1; // char size representation in the buffer (size of the escape), surrogate pair are handled
    protected final char[] codePointBuffer; // code point buffer holding character representation of the code point, limited to 2

    public UnicodeTranslates(char[] value) {
        this.value = value;
        this.codePointBuffer = new char[2];
    }

    public char peek() {
        return this.peek(0);
    }

    public char read() {
        if (!this.canRead()) {
            throw new RuntimeException("Expected to read a new character at " + this.cursor);
        }
        char c = this.peek(0);
        this.incrCursor();
        return c;
    }

    public boolean match(char c) {
        if (this.canRead() && this.peek() == c) {
            this.incrCursor();
            return true;
        }

        return false;
    }

    public boolean match(String str) {
        int size = str.length();
        if (!this.canRead(size)) {
            return false;
        }

        int previousCursor = this.cursor;
        // with unicode escape there's no easy way to do this without resetting the cursor later due to mismatch
        // the fact that multiple unicode marker ('u') are allowed doesn't help either
        for (int i = 0; i < size; i++) {
            if (!this.canRead() || this.peek() != str.charAt(i)) {
                this.cursor = previousCursor;
                return false;
            }

            this.incrCursor();
        }

        return true;
    }

    public void incrCursor() {
        this.cursor += this.charSize;
    }

    public char peek(int offset) {
        char c = this.value[this.cursor + offset];
        if (c != '\\') {
            this.charSize = 1;
            return c;
        }

        int prefixSize = 1;
        while (this.canRead(offset + prefixSize + 1) && this.peek(offset + prefixSize) == 'u') { // match as many unicode marker ('u') as possible
            prefixSize++;
        }

        if (prefixSize > 1) { // found unicode sequence -> parse 4 hexadecimal digits
            if (!this.canRead(offset + prefixSize + 4)) {
                throw new RuntimeException("Invalid java source, found a malformed unicode escape sequence: missing/incomplete code point value");
            }

            int codePoint = 0;
            for (int i = 0; i < 4; i++) {
                char c2 = this.peek(offset + prefixSize + i);
                int digit = Character.digit(c2, 16);
                if (digit == -1) {
                    throw new RuntimeException("Invalid java source, found a malformed unicode escape sequence: invalid hexadecimal digit '%c'".formatted(c2));
                }

                codePoint = codePoint << 4 | digit;
            }

            this.charSize = prefixSize + 4;
            return (char) codePoint;
        }

        this.charSize = 1;
        return c; // other escape
    }

    public int peekPoint() {
        return this.peekPoint(0);
    }

    public int peekPoint(int offset) {
        char hi = this.peek(offset);
        int size = this.charSize;
        this.codePointBuffer[0] = hi;
        if (!Character.isSurrogate(hi)) {
            this.codePointBuffer[1] = '\0';
            return hi;
        }

        if (Character.isHighSurrogate(hi)) {
            if (!this.canRead(offset + size + 1)) {
                throw new RuntimeException("Invalid java source, found a high surrogate (\\u%04X) without its sibling".formatted((int) hi));
            }

            char lo = this.peek(offset + size);
            if (isNewline(lo)) { // special case since the buffer is read entirely with its newline
                throw new RuntimeException("Invalid java source, found a high surrogate (\\u%04X) without its sibling".formatted((int) hi));
            }

            if (!Character.isLowSurrogate(lo)) {
                throw new RuntimeException("Invalid java source, found a malformed surrogate pair: low surrogate is invalid (\\u%04X)".formatted((int) lo));
            }

            this.codePointBuffer[1] = lo;
            size += this.charSize;
            this.charSize = size;

            return Character.toCodePoint(hi, lo);
        } else {
            throw new RuntimeException("Invalid java source, found a low surrogate (\\u%04X) before its sibling".formatted((int) hi));
        }
    }

    public boolean canRead() {
        return this.canRead(1);
    }

    public boolean canRead(int length) {
        return this.cursor + length <= this.value.length;
    }

    protected boolean isSpace(char c) {
        return c == ' ' ||
            c == '\t' ||
            c == '\f';
    }

    protected boolean isNewline(char c) {
        return c == '\n' || c == '\r'; // todo check cursor for windows double character \r\n (low priority)
    }

    protected boolean isUnicodeEscape() {
        return this.charSize > 2;
    }

    public int getCursor() {
        return this.cursor;
    }

    // doesn't copy the array
    public char[] toCharArray() {
        return this.value;
    }
}
