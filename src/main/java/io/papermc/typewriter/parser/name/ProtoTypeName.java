package io.papermc.typewriter.parser.name;

import io.papermc.typewriter.parser.ParserException;
import io.papermc.typewriter.parser.StringReader;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.lang.model.SourceVersion;

import java.util.function.Predicate;

public class ProtoTypeName { // todo add test for this class for non finite type name

    public static final char IDENTIFIER_SEPARATOR = '.';

    private final Predicate<StringReader> cleaner;

    protected StringBuilder name;
    private boolean separatorExpected;
    protected boolean checkStartId = true;
    protected StringReader previousLine, printedLine; // previous processed line, previous printed identifier line
    private int validUntil = 0;

    public ProtoTypeName() {
        this(line -> false);
    }

    public ProtoTypeName(Predicate<StringReader> cleaner) {
        // cleaner is used to skip stuff like : net/* hi */./**/kyori.adventure.translation/**/.Translatable within the type name
        this.cleaner = cleaner;
    }

    // return the number of valid chars read or -1 to stop reading
    public int tryAdvance(int codePoint, StringReader line) {
        if (this.name != null) {
            if (this.checkStartId) { // had a dot before
                if (this.cleaner.test(line)) {
                    return 0;
                }
            } else {
                if (Character.isIdentifierIgnorable(codePoint)) {
                    return Character.charCount(codePoint);
                }

                if (this.separatorExpected || this.previousLine != line) { // on newline expect a dot, identifier cannot be split in two
                    this.separatorExpected = false;
                    if (codePoint != IDENTIFIER_SEPARATOR) {
                        return -1;
                    }
                }
            }
        }

        this.previousLine = line;

        if (!this.isValid(codePoint)) {
            if (this.checkStartId) {
                return -1; // cleaner already ran before
            }

            if (codePoint != IDENTIFIER_SEPARATOR) {
                if (this.cleaner.test(line)) {
                    this.separatorExpected = true;
                    return 0;
                } else {
                    return -1;
                }
            }
        }

        if (codePoint != IDENTIFIER_SEPARATOR) {
            this.printedLine = line;
        }

        char[] chars = Character.toChars(codePoint);
        this.append(chars);
        this.checkStartId = codePoint == IDENTIFIER_SEPARATOR;

        if (this.checkStartId) {
            // check identifier validity one at a time once fully read (allow to point to the correct line in the exception)
            this.checkIdentifier(1);
        }

        return chars.length;
    }

    protected boolean isValid(int codePoint) {
        return this.checkStartId ? Character.isJavaIdentifierStart(codePoint) : Character.isJavaIdentifierPart(codePoint);
    }

    public void checkIdentifier(int offset) {
        String identifier = this.name.substring(this.validUntil, this.name.length() - offset);
        this.validateIdentifier(identifier);
        this.validUntil += identifier.length() + offset;

        if (this.validUntil > this.name.length()) { // trim if any child class change the name for whatever reason
            this.validUntil = this.name.length();
        }
    }

    protected void validateIdentifier(String identifier) {
        if (identifier.isEmpty()) {
            throw new ParserException("Invalid java source, type name contains a syntax error", this.printedLine);
        }
        if (SourceVersion.isKeyword(identifier) && !this.ignoreKeyword(identifier)) {
            throw new ParserException("Invalid java source, type name contains a reserved keyword", this.printedLine);
        }
    }

    protected boolean ignoreKeyword(String keyword) {
        return false;
    }

    protected void append(char... chars) {
        if (this.name == null) {
            this.name = new StringBuilder();
        }
        this.name.append(chars);
    }

    public @Nullable String getTypeName() {
        if (this.name == null) {
            return null;
        }

        return this.name.toString();
    }


    public void throwIfMalformed() {
        @Nullable ParserException error = this.checkIntegrity();
        if (error != null) {
            throw error;
        }
    }

    public @Nullable ParserException checkIntegrity() {
        @Nullable String name = this.getTypeName();
        if (name == null || name.isEmpty()) {
            return new ParserException("Invalid java source, type name is empty", this.previousLine);
        }

        if (this.validUntil != name.length()) {
            // check last identifier when terminator is not reach (e.g. annotation)
            this.checkIdentifier(0);
        }

        return null;
    }
}
