package io.papermc.typewriter.parser;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Predicate;

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

    public int peekPoint() {
        return this.string.codePointAt(this.cursor);
    }

    public int skipWhitespace() {
        int i = 0;
        while (canRead() && Character.isWhitespace(peek())) {
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

    public void skipStringUntil(final char terminator) {
        while (this.canRead() && this.peek() != terminator) {
            this.skip();
        }
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
        if (!this.canRead(size)) {
            return false;
        }

        int delta = 0;
        int previousCursor = this.cursor;
        while (delta < size && value.charAt(delta) == this.peek()) {
            this.skip();
            delta++;
        }
        if (delta == size) {
            return true;
        }

        this.setCursor(previousCursor);
        return false;
    }

    public String readStringUntil(final char terminator) {
        final int start = this.cursor;
        this.skipStringUntil(terminator);
        return this.string.substring(start, this.cursor);
    }

    // cleaner is used to skip stuff like : net/* hi */./**/kyori.adventure.translation/**/.Translatable within the type name
    public ProtoTypeName getPartNameUntil(final char terminator, final Predicate<StringReader> cleaner,
                                          @Nullable ProtoTypeName currentName) { // this break the concept of this a class a bit but it's not worth making a code point equivalent for only this method
        boolean hasCleaner = cleaner != null;
        boolean checkStart = currentName == null || currentName.shouldCheckStartIdentifier();
        while (this.canRead()) {
            int c = this.peekPoint();
            if (c == terminator) {
                break;
            }

            if (checkStart) { // had a dot before
                if (hasCleaner && cleaner.test(this)) {
                    continue;
                }
            }

            boolean isJavaIdChar = checkStart ? Character.isJavaIdentifierStart(c) : Character.isJavaIdentifierPart(c);
            if (!isJavaIdChar && (checkStart || c != ProtoTypeName.IDENTIFIER_SEPARATOR)) {
                if (hasCleaner && cleaner.test(this)) {
                    if (currentName != null) {
                        currentName.expectIdTerminator();
                    }
                    continue;
                } else {
                    break;
                }
            }

            char[] chars = Character.toChars(c);
            if (currentName != null) {
                if (!currentName.append(chars)) {
                    break;
                }
            } else {
                currentName = new ProtoTypeName(chars);
            }
            this.cursor += chars.length;
            checkStart = c == ProtoTypeName.IDENTIFIER_SEPARATOR;
        }
        return currentName;
    }
}
