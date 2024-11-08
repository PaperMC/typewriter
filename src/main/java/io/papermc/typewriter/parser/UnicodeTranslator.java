package io.papermc.typewriter.parser;

import io.papermc.typewriter.parser.exception.LexerException;

public class UnicodeTranslator {
    private final char[] input;

    private int charSize = 1; // char size representation in the buffer (size of the escape), surrogate pair are handled
    protected final char[] codePointCache = new char[2]; // code point cache holding character representation of the code point, limited to 2

    private int cursor;
    private int column; // character count (0-indexed) after unicode translation
    private int row = 1; // line count

    public UnicodeTranslator(char[] input) {
        this.input = input;
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
        this.column++;
    }

    public char read() {
        if (!this.canRead()) {
            throw new LexerException("Expected to read a new character", this);
        }
        char c = this.peek(0);
        this.incrCursor();
        return c;
    }

    public char peek() {
        return this.peek(0);
    }

    private char peek(int offset) {
        char c = this.input[this.cursor + offset];
        if (c != '\\') {
            this.charSize = 1;
            return c;
        }

        int prefixSize = 1;
        while (this.canRead(offset + prefixSize + 1) && this.input[this.cursor + offset + prefixSize] == 'u') { // match as many unicode marker ('u') as possible
            prefixSize++;
        }

        if (prefixSize > 1) { // found unicode sequence -> parse 4 hexadecimal digits
            if (!this.canRead(offset + prefixSize + 4)) {
                throw new LexerException("Invalid java source, found a malformed unicode escape sequence: missing/incomplete code point value", this);
            }

            int codePoint = 0;
            for (int i = 0; i < 4; i++) {
                char c2 = this.input[this.cursor + offset + prefixSize + i];
                int digit = Character.digit(c2, 16);
                if (digit == -1) {
                    int contiguousSize = isUnicodeEscape() ? 1 : 0;
                    throw new LexerException("Invalid java source, found a malformed unicode escape sequence: invalid hexadecimal digit '%c'".formatted(c2), this, contiguousSize + prefixSize + i);
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
        char hi = this.peek();
        int size = this.charSize;
        this.codePointCache[0] = hi;
        if (!Character.isSurrogate(hi)) {
            this.codePointCache[1] = '\0';
            return hi;
        }

        if (Character.isHighSurrogate(hi)) {
            if (!this.canRead(size + 1)) {
                throw new LexerException("Invalid java source, found a high surrogate (\\u%04X) without its sibling".formatted((int) hi), this);
            }

            char lo = this.peek(size);
            if (isLineTerm(lo)) { // special case since the buffer is read entirely with its newline
                throw new LexerException("Invalid java source, found a high surrogate (\\u%04X) without its sibling".formatted((int) hi), this);
            }

            if (!Character.isLowSurrogate(lo)) {
                throw new LexerException("Invalid java source, found a malformed surrogate pair: low surrogate is invalid (\\u%04X)".formatted((int) lo), this);
            }

            this.codePointCache[1] = lo;
            size += this.charSize;
            this.charSize = size;

            return Character.toCodePoint(hi, lo);
        } else {
            throw new LexerException("Invalid java source, found a low surrogate (\\u%04X) before its sibling".formatted((int) hi), this);
        }
    }

    public boolean canRead() {
        return this.canRead(1);
    }

    public boolean canRead(int length) {
        return this.cursor + length <= this.input.length;
    }

    protected boolean isSpace(char c) {
        return c == ' ' ||
            c == '\t' ||
            c == '\f';
    }

    protected boolean isLineTerm(char c) {
        return c == '\n' || c == '\r';
    }

    public void skipLineTerm() {
        match('\r');
        match('\n');
        this.visitLineTerminator();
    }

    protected boolean isUnicodeEscape() {
        return this.charSize > 2;
    }

    public int getCursor() {
        return this.cursor;
    }

    protected void visitLineTerminator() {
        this.row++;
        this.column = 0;
    }

    public int getColumn() {
        return this.column;
    }

    public int getRow() {
        return this.row;
    }

    // doesn't copy the array
    public char[] toCharArray() {
        return this.input;
    }
}
