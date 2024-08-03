package io.papermc.typewriter.parser;

import io.papermc.typewriter.parser.exception.LexerException;
import io.papermc.typewriter.parser.token.CharSequenceBlockToken;
import io.papermc.typewriter.parser.token.CharSequenceToken;
import io.papermc.typewriter.parser.token.RawToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;
import org.jetbrains.annotations.ApiStatus;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class Lexer extends UnicodeTranslator {

    private final StringBuilder buffer; // generic buffer for single line element or used as temporary storage before being pushed into line buffer
    private final List<String> lineBuffer; // line buffer for multi line block

    public Lexer(char[] input) {
        super(input);
        this.buffer = new StringBuilder();
        this.lineBuffer = new ArrayList<>(5);
    }

    public static Lexer fromReader(Reader reader) throws IOException { // might find a better way maybe more lazy too
        CharArrayWriter w = new CharArrayWriter();
        int code;
        while ((code = reader.read()) != -1) {
            w.append((char) code);
        }
        return new Lexer(w.toCharArray());
    }

    // keyword/var/type name etc.
    public void readIdentifier() {
        while (this.canRead()) {
            int code = this.peekPoint();
            if (Character.isIdentifierIgnorable(code)) {
                this.incrCursor();
                continue;
            }

            if (!Character.isJavaIdentifierPart(code)) {
                break;
            }

            this.appendCodePoint();
            this.incrCursor();
        }
    }

    public void appendCodePoint() {
        this.buffer.append(this.codePointBuffer[0]);
        if (this.codePointBuffer[1] != '\0') {
            this.buffer.append(this.codePointBuffer[1]);
        }
    }

    // handle all regular escape, unicode escape are done before and might form another regular escape after the first conversion
    public void appendLiteral(char c) {
        if (c != '\\') {
            this.buffer.append(c);
            return;
        }

        char initialChar = this.read();
        char translatedChar;
        switch (initialChar) {
            case 't':
                translatedChar = '\t';
                break;
            case 'f':
                translatedChar = '\f';
                break;
            case 'b':
                translatedChar = '\b';
                break;
            case 's':
                translatedChar = '\s';
                break;
            case '\\':
            case '"':
            case '\'':
                translatedChar = initialChar;
                break;
            case 'r':
            case 'n':
                translatedChar = '\n'; // normalize
                break;
            default:
                if (initialChar >= '0' && initialChar <= '7') { // octal escape (limited to \377)
                    int codePoint = Character.digit(initialChar, 8);
                    char c3 = this.peek();
                    if (c3 >= '0' && c3 <= '7') {
                        this.incrCursor();
                        codePoint = codePoint << 3 | Character.digit(c3, 8);
                        char c4 = this.peek();
                        if (initialChar <= '3' && c4 >= '0' && c4 <= '7') {
                            this.incrCursor();
                            codePoint = codePoint << 3 | Character.digit(c4, 8);
                        }
                    }
                    translatedChar = (char) codePoint;
                    break;
                }
                throw new IllegalStateException("Invalid char escape");
        }
        this.buffer.append(translatedChar);
    }

    // " " """ """
    public void readString(boolean paragraph) {
        boolean firstLine = true;
        while (this.canRead()) {
            char c = this.peek();

            if (paragraph) {
                if (this.buffer.isEmpty()) {
                    if (isSpace(c)) { // todo this doesn't keep initial indent after the base level (need somehow to track newline cursors)
                        this.incrCursor();
                        continue;
                    }
                }
            } else {
                if (isNewline(c)) {
                    throw new LexerException("Illegal newline inside string literal after \"%s\"".formatted(this.buffer.toString()), this);
                }
            }

            final boolean reachEnd;
            if (paragraph) {
                reachEnd = match("\"\"\"");
            } else {
                reachEnd = match('"');
            }

            if ((paragraph && isNewline(c)) || reachEnd) { // this logic is too convoluted split more block
                if (paragraph) {
                    String line = this.readBuffer();
                    if (reachEnd || firstLine) { // ignore empty line for start and end
                        firstLine = false;
                        if (!line.isEmpty()) {
                            this.lineBuffer.add(line);
                        }
                    } else {
                        this.lineBuffer.add(line);
                    }
                }
                if (reachEnd) {
                    break;
                }
                // end of line
                this.incrCursor();
                this.visitLineTerminator();
            } else {
                this.appendLiteral(this.read());
            }
        }
    }

    //
    public void readSingleLineComment() {
        while (this.canRead()) {
            char c = this.peek();
            if (isNewline(c)) {
                break;
            }

            this.buffer.append(c);
            this.incrCursor();
        }
    }

    /// markdown
    /// javadoc `sparkl`
    @ApiStatus.Experimental
    public void readMarkdownJavadoc() { // JEP 467
        boolean expectPrefix = false; // first prefix is already checked before
        while (this.canRead()) {
            char c = this.peek();
            if (this.buffer.isEmpty()) {
                if (expectPrefix) {
                    if (isSpace(c)) { // trim leading space before prefix ///
                        this.incrCursor();
                        continue;
                    }

                    if (match("///")) {
                        expectPrefix = false;
                        continue;
                    } else {
                        break;
                    }
                }
            }

            if (isNewline(c)) {
                String line = this.readBuffer();
                this.lineBuffer.add(line);
                this.incrCursor();
                this.visitLineTerminator();
                expectPrefix = true;
                continue;
            }

            this.buffer.append(c);
            this.incrCursor();
        }
    }

    /*(*)
    (*)
     */
    public void readComment(boolean javadoc) {
        boolean firstLine = true;
        while (this.canRead()) {
            if (match("*/")) {
                String line = this.readBuffer();
                if (!line.isEmpty()) { // ignore empty line at end
                    this.lineBuffer.add(line);
                }
                break;
            }

            char c = this.peek();
            if (javadoc && this.buffer.isEmpty()) {
                if (isSpace(c) || c == '*') {
                    this.incrCursor();
                    continue;
                }
            }

            if (isNewline(c)) {
                String line = this.readBuffer();
                if (firstLine) {
                    if (!line.isEmpty()) { // ignore empty line at start
                        this.lineBuffer.add(line);
                    }
                    firstLine = false;
                } else {
                    this.lineBuffer.add(line);
                }
                this.incrCursor();
                this.visitLineTerminator();
                continue;
            }

            this.buffer.append(c);
            this.incrCursor();
        }
    }

    public String readBuffer() {
        String value = this.buffer.toString();
        this.buffer.delete(0, this.buffer.length());
        return value;
    }

    public List<String> readLineBuffer() {
        List<String> lines = List.copyOf(this.lineBuffer);
        this.lineBuffer.clear();
        return lines;
    }

    public Token readToken() {
        TokenType type = null;
        int startPos = 0;
        int startColumn = 0;
    loop:
        while (this.canRead()) {
            char c = this.peek();
            switch (c) {
                case ' ':
                case '\t':
                case '\f':
                    this.incrCursor();
                    break;
                case '\n':
                    this.incrCursor();
                    this.visitLineTerminator();
                    break;
                case '\r':
                    this.incrCursor();
                    match('\n');
                    this.visitLineTerminator();
                    break;
                case '/':
                    startPos = this.cursor;
                    startColumn = this.getColumn();
                    this.incrCursor();
                    if (match('/')) {
                        if (match('/')) {
                            type = TokenType.MARKDOWN_JAVADOC;
                            this.readMarkdownJavadoc();
                        } else {
                            type = TokenType.SINGLE_COMMENT;
                            this.readSingleLineComment();
                        }
                        break loop;
                    }

                    if (match('*')) {
                        if (match('*')) {
                            type = TokenType.JAVADOC;
                            if (match('/')) {
                                // empty javadoc also seen as empty single line comment
                                break loop;
                            }
                        } else {
                            type = TokenType.COMMENT;
                        }
                        this.readComment(type == TokenType.JAVADOC);
                        break loop;
                    }
                    break;
                case '\'':
                    type = TokenType.CHAR;
                    startPos = this.cursor;
                    startColumn = this.getColumn();
                    this.incrCursor();
                    this.appendLiteral(this.read());
                    if (!match('\'')) {
                        throw new LexerException("Unbalanced quote for char literal " + this.buffer.toString(), this);
                    }
                    break loop;
                case '"':
                    startPos = this.cursor;
                    startColumn = this.getColumn();
                    this.incrCursor();
                    if (match("\"\"")) {
                        if (!match("\r\n") && !match('\n') && !match('\r')) {
                            throw new LexerException("Expect a new line after paragraph open delimiter", this);
                        }

                        type = TokenType.PARAGRAPH;
                    } else {
                        type = TokenType.STRING;
                    }
                    this.readString(type == TokenType.PARAGRAPH);
                    break loop;
                case '(':
                    type = TokenType.LPAREN;
                    startPos = this.cursor;
                    startColumn = this.getColumn();
                    this.incrCursor();
                    break loop;
                case ')':
                    type = TokenType.RPAREN;
                    startPos = this.cursor;
                    startColumn = this.getColumn();
                    this.incrCursor();
                    break loop;
                case '{':
                    type = TokenType.LSCOPE;
                    startPos = this.cursor;
                    startColumn = this.getColumn();
                    this.incrCursor();
                    break loop;
                case '}':
                    type = TokenType.RSCOPE;
                    startPos = this.cursor;
                    startColumn = this.getColumn();
                    this.incrCursor();
                    break loop;
                case '[':
                    type = TokenType.LBRACKET;
                    startPos = this.cursor;
                    startColumn = this.getColumn();
                    this.incrCursor();
                    break loop;
                case ']':
                    type = TokenType.RBRACKET;
                    startPos = this.cursor;
                    startColumn = this.getColumn();
                    this.incrCursor();
                    break loop;
                case '@':
                    type = TokenType.AT_SIGN;
                    startPos = this.cursor;
                    startColumn = this.getColumn();
                    this.incrCursor();
                    break loop;
                case '*':
                    type = TokenType.STAR;
                    startPos = this.cursor;
                    startColumn = this.getColumn();
                    this.incrCursor();
                    break loop;
                case '.':
                    type = TokenType.DOT;
                    startPos = this.cursor;
                    startColumn = this.getColumn();
                    this.incrCursor();
                    break loop;
                case ',':
                    type = TokenType.CO;
                    startPos = this.cursor;
                    startColumn = this.getColumn();
                    this.incrCursor();
                    break loop;
                case ';':
                    type = TokenType.SECO;
                    startPos = this.cursor;
                    startColumn = this.getColumn();
                    this.incrCursor();
                    break loop;
                default:
                    if (Character.isJavaIdentifierStart(this.peekPoint())) {
                        type = TokenType.IDENTIFIER;
                        startPos = this.cursor;
                        startColumn = this.getColumn();
                        this.appendCodePoint();
                        this.incrCursor();
                        this.readIdentifier();
                        break loop;
                    }
                    this.incrCursor();
                    break;
            }
        }

        if (!this.canRead()) {
            startPos = this.cursor;
            startColumn = this.getColumn();
            type = TokenType.EOI;
        }

        if (type == null) {
            throw new LexerException("Unknown token found", this);
        }

        if (CharSequenceToken.TYPES.contains(type)) {
            String value = this.readBuffer();
            if (type == TokenType.IDENTIFIER) {
                type = TokenType.fromName(value, type);
            }
            return new CharSequenceToken(type, value, this.getRow(), startColumn, this.getColumn(), startPos, this.cursor);
        }

        if (CharSequenceBlockToken.TYPES.contains(type)) {
            return new CharSequenceBlockToken(type, this.readLineBuffer(), this.getRow(), startColumn, startPos, this.cursor);
        }

        return new RawToken(type, this.getRow(), startColumn, startPos);
    }

    public static boolean isWhitespace(int codePoint) {
        if (!Character.isBmpCodePoint(codePoint)) {
            return false; // just in case unicode is extended
        }

        return codePoint == ' ' ||
            codePoint == '\t' ||
            codePoint == '\f' ||
            codePoint == '\n' ||
            codePoint == '\r';
    }
}
