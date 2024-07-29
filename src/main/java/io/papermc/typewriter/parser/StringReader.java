package io.papermc.typewriter.parser;

// based on brigadier string reader with some extra/removed features for rewriter
public class StringReader {

    private final String string;
    private int cursor;

    public StringReader(final StringReader other) {
        this.string = other.string;
        this.cursor = other.cursor;
    }

    public StringReader(final String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

    public void setCursor(final int cursor) {
        this.cursor = cursor;
    }

    public int getRemainingLength() {
        return string.length() - cursor;
    }

    public int getTotalLength() {
        return string.length();
    }

    public int getCursor() {
        return cursor;
    }

    public String getRead() {
        return string.substring(0, cursor);
    }

    public String getRemaining() {
        return string.substring(cursor);
    }

    public boolean canRead(final int length) {
        return cursor + length <= string.length();
    }

    public boolean canRead() {
        return canRead(1);
    }

    public char peek() {
        return string.charAt(cursor);
    }

    public char peek(final int offset) {
        return string.charAt(cursor + offset);
    }

    public char read() {
        return string.charAt(cursor++);
    }

    public void skip() {
        cursor++;
    }

    // new features

    public int skipWhitespace() {
        int i = 0;
        while (this.canRead() && Character.isWhitespace(this.peek())) {
            this.skip();
            i++;
        }
        return i;
    }

    public int skipChars(final char value) {
        int i = 0;
        while (this.canRead() && this.peek() == value) {
            this.skip();
            i++;
        }
        return i;
    }

    public void skipUntil(final char terminator) {
        while (this.canRead() && this.peek() != terminator) {
            this.skip();
        }
    }

    public String readUntil(final char terminator) {
        final int start = this.cursor;
        this.skipUntil(terminator);
        return this.string.substring(start, this.cursor);
    }

    public boolean trySkipWhitespace(final int size) {
        if (!this.canRead(size)) {
            return false;
        }

        int delta = 0;
        int previousCursor = this.cursor;
        while (delta < size && Character.isWhitespace(this.peek())) {
            this.skip();
            delta++;
        }
        if (delta == size) {
            return true;
        }

        this.setCursor(previousCursor);
        return false;
    }

    public boolean trySkipChars(final int size, final char value) {
        if (!this.canRead(size)) {
            return false;
        }

        int delta = 0;
        int previousCursor = this.cursor;
        while (delta < size && this.peek() == value) {
            this.skip();
            delta++;
        }
        if (delta == size) {
            return true;
        }

        this.setCursor(previousCursor);
        return false;
    }

    public boolean trySkipString(final String value) {
        int size = value.length();
        if (this.string.regionMatches(this.cursor, value, 0, size)) {
            this.cursor += size;
            return true;
        }

        return false;
    }
}
