package io.papermc.typewriter.parser;

import io.papermc.typewriter.parser.exception.LexerException;
import io.papermc.typewriter.parser.token.pos.AbsolutePos;
import io.papermc.typewriter.parser.token.pos.TokenCapture;
import io.papermc.typewriter.parser.token.pos.TokenRecorder;
import io.papermc.typewriter.parser.token.pos.TokenSnapshot;
import io.papermc.typewriter.parser.token.CharSequenceBlockToken;
import io.papermc.typewriter.parser.token.CharSequenceToken;
import io.papermc.typewriter.parser.token.CharToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.ApiStatus;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Lexer extends UnicodeTranslator implements Tokenizer {

    private final StringBuilder buffer; // generic buffer for single line element or used as temporary storage before being pushed into line buffer
    private final List<String> lineBuffer; // line buffer for multi line block
    private RelativeTextBlock textBlock; // text block support

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
    private void readIdentifier() {
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

    private void appendCodePoint() {
        this.buffer.append(this.codePointCache[0]);
        if (this.codePointCache[1] != '\0') {
            this.buffer.append(this.codePointCache[1]);
        }
    }

    // handle all regular escape, unicode escape are done before and might form another regular escape after the first conversion
    private void appendLiteral(char c, @Nullable RelativeTextBlock textBlock) {
        if (c != '\\') {
            this.buffer.append(c);
            return;
        }

        boolean skipIncrement = false;
        char initialChar = this.peek();
        if (textBlock != null) {
            if (isLineTerm(initialChar)) { // \ is used to consider the next line in text block as the same line
                textBlock.notifyEscape(RelativeTextBlock.EscapeType.NEW_LINE);
                return;
            }
            if (initialChar == 's') {
                this.incrCursor();
                skipIncrement = true;
                if (isLineTerm(this.peek())) {
                    textBlock.notifyEscape(RelativeTextBlock.EscapeType.SPACE);
                }
            }
        }

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
                translatedChar = ' ';
                break;
            case '\\':
            case '"':
            case '\'':
                translatedChar = initialChar;
                break;
            case 'r':
                translatedChar = '\r';
                break;
            case 'n':
                translatedChar = '\n';
                break;
            default:
                if (initialChar >= '0' && initialChar <= '7') { // octal escape (limited to \377)
                    this.incrCursor();
                    skipIncrement = true;
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
                throw new LexerException("Invalid char escape (" + initialChar + ")", this);
        }
        if (!skipIncrement) {
            this.incrCursor();
        }
        if (textBlock != null && isSpace(translatedChar)) { // special case for text block escaped space doesn't count as a blank line/indent
            textBlock.notifySpaceEscapeAt(this.buffer.length());
        }
        this.buffer.append(translatedChar);
    }

    private class RelativeTextBlock {

        record TextLine(String content, int effectiveSize, EscapeType escapeType) {

            public void addTo(StringBuilder builder, int leadingIndent) {
                if (this.effectiveSize > 0) {
                    builder.append(this.content, leadingIndent, Math.max(this.effectiveSize, leadingIndent));
                }
            }

            public String get(int leadingIndent) {
                if (this.effectiveSize == 0) {
                    return "";
                }
                return this.content.substring(leadingIndent, Math.max(this.effectiveSize, leadingIndent));
            }
        }

        enum EscapeType { // relative to a text line
            SPACE,
            NEW_LINE,
            NONE
        }

        private final List<TextLine> lines = new ArrayList<>();

        private EscapeType currentEscapeType = EscapeType.NONE;
        private final Set<Integer> spaceEscapes = new HashSet<>();

        private final StringBuilder longLine = new StringBuilder();

        private Integer leadingIndent;

        public void notifyEscape(EscapeType type) {
            this.currentEscapeType = type;
        }

        public void notifySpaceEscapeAt(int pos) { // \s \t \f
            this.spaceEscapes.add(pos);
        }

        public void add(String line) { // regular escape are already translated
            int size = line.length();

            // compute leading indent
            int i;
            for (i = 0; i < size && !this.spaceEscapes.contains(i) && Lexer.this.isSpace(line.charAt(i)); i++) {
            }

            if (i != size || this.currentEscapeType == EscapeType.NEW_LINE) {
                // ignore blank line (since new line escape is cut before that make sure it's not considered as blank
                // space escape are already not considered as blank with the spaceEscapes list)
                if (this.leadingIndent == null) {
                    this.leadingIndent = i;
                } else {
                    this.leadingIndent = Math.min(i, this.leadingIndent);
                }
            }

            // compute trailing indent
            int effectiveSize = size; // line size without trailing indent
            if (this.currentEscapeType == EscapeType.NONE) {
                for (i = size - 1; i >= 0 && !this.spaceEscapes.contains(i) && Lexer.this.isSpace(line.charAt(i)); i--) {
                    effectiveSize--;
                }
            }

            this.lines.add(new TextLine(line, effectiveSize, this.currentEscapeType));

            this.currentEscapeType = EscapeType.NONE;
            this.spaceEscapes.clear();
        }

        public void getIn(List<String> output) {
            for (TextLine line : this.lines) {
                if (line.escapeType() == EscapeType.NEW_LINE) {
                    line.addTo(this.longLine, this.leadingIndent);
                } else {
                    if (this.longLine.isEmpty()) {
                        output.add(line.get(this.leadingIndent));
                    } else {
                        line.addTo(this.longLine, this.leadingIndent);
                        output.add(this.longLine.toString());
                        this.longLine.delete(0, this.longLine.length());
                    }
                }
            }
        }
    }

    // " "
    private void readString() {
        while (this.canRead()) {
            char c = this.peek();
            if (isLineTerm(c)) {
                throw new LexerException("Illegal newline inside string literal after \"%s\"".formatted(this.buffer.toString()), this);
            }

            if (match('"')) {
                break;
            }

            this.incrCursor();
            this.appendLiteral(c, null);
        }
    }

    // """ """
    private void readParagraph() {
        // skip optional space between open delimiter and line terminator
        while (this.canRead()) {
            if (!isSpace(this.peek())) {
                break;
            }

            this.incrCursor();
        }

        if (this.canRead() && isLineTerm(this.peek())) {
            this.skipLineTerm();
        } else {
            throw new LexerException("Expect a new line after paragraph open delimiter", this);
        }
        this.textBlock = new RelativeTextBlock();

        while (this.canRead()) {
            char c = this.peek();
            final boolean reachEnd = match("\"\"\"");
            if (isLineTerm(c) || reachEnd) {
                String line = this.readBuffer();
                this.textBlock.add(line);

                if (reachEnd) {
                    this.textBlock.getIn(this.lineBuffer);
                    break;
                }
                // end of line
                this.skipLineTerm();
            } else {
                this.incrCursor();
                this.appendLiteral(c, this.textBlock);
            }
        }
    }

    //
    private void readSingleLineComment() {
        while (this.canRead()) {
            char c = this.peek();
            if (isLineTerm(c)) {
                break;
            }

            this.buffer.append(c);
            this.incrCursor();
        }
    }

    /// markdown
    /// javadoc `sparkl`
    @ApiStatus.Experimental
    private void readMarkdownJavadoc(TokenRecorder.Constant tokenPos) { // JEP 467
        boolean expectPrefix = false; // first prefix is already checked before
        while (this.canRead()) {
            char c = this.peek();
            if (this.buffer.isEmpty()) {
                if (isSpace(c)) { // trim leading space before content [space]///[space][content]
                    this.incrCursor();
                    continue;
                }

                if (expectPrefix) {
                    if (match("///")) {
                        expectPrefix = false;
                        continue;
                    } else {
                        break;
                    }
                }
            }

            if (isLineTerm(c)) {
                String line = this.readBuffer();
                this.lineBuffer.add(stripTrailingSpace(line));
                tokenPos.end(); // the end is not clearly defined until the very end
                this.skipLineTerm();
                expectPrefix = true;
                continue;
            }

            this.buffer.append(c);
            this.incrCursor();
        }
    }

    /**
     *
     */
    private void readJavadoc() {
        boolean firstLine = true;
        while (this.canRead()) {
            if (match("*/")) {
                String line = this.readBuffer();
                if (!isBlank(line)) { // ignore blank line at end
                    this.lineBuffer.add(stripTrailingSpace(line));
                }
                break;
            }

            char c = this.peek();
            if (this.buffer.isEmpty()) {
                if (isSpace(c) || c == '*') {
                    this.incrCursor();
                    continue;
                }
            }

            if (isLineTerm(c)) {
                String line = this.readBuffer();
                if (!firstLine || !isBlank(line)) { // ignore blank line at start
                    this.lineBuffer.add(stripTrailingSpace(line));
                }
                firstLine = false;
                this.skipLineTerm();
                continue;
            }

            this.buffer.append(c);
            this.incrCursor();
        }
    }

    /*

     */
    private void readComment() {
        this.textBlock = new RelativeTextBlock();
        boolean firstLine = true;
        while (this.canRead()) {
            if (match("*/")) {
                String line = this.readBuffer();
                if (!isBlank(line)) { // ignore blank line at end
                    this.textBlock.add(line);
                }
                this.textBlock.getIn(this.lineBuffer);
                break;
            }

            char c = this.peek();
            if (isLineTerm(c)) {
                String line = this.readBuffer();
                if (!firstLine || !isBlank(line)) { // ignore blank line at start
                    this.textBlock.add(line);
                }
                firstLine = false;
                this.skipLineTerm();
                continue;
            }

            this.buffer.append(c);
            this.incrCursor();
        }
    }

    private String readBuffer() {
        String value = this.buffer.toString();
        this.buffer.delete(0, this.buffer.length());
        return value;
    }

    private List<String> readLineBuffer() {
        List<String> lines = List.copyOf(this.lineBuffer);
        this.lineBuffer.clear();
        return lines;
    }

    @Override
    public Token readToken() {
        TokenType type = null;
        TokenSnapshot.Constant<Lexer> snapshot = TokenRecorder.LEXER_INSTANT;
        TokenRecorder.Constant tokenPos = snapshot.record(this);
        AbsolutePos singlePos = null;
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
                    tokenPos.begin();
                    this.incrCursor();
                    if (match('/')) {
                        if (match('/')) {
                            type = TokenType.MARKDOWN_JAVADOC;
                            this.readMarkdownJavadoc(tokenPos);
                        } else {
                            type = TokenType.SINGLE_COMMENT;
                            this.readSingleLineComment();
                        }
                        break loop;
                    }

                    if (match('*')) {
                        if (match('*')) {
                            if (match('/')) {
                                // empty comment /**/ shouldn't be interpreted as javadoc
                                type = TokenType.COMMENT;
                                break loop;
                            }
                            type = TokenType.JAVADOC;
                            this.readJavadoc();
                        } else {
                            type = TokenType.COMMENT;
                            this.readComment();
                        }
                        break loop;
                    }
                    break;
                case '\'':
                    type = TokenType.CHAR;
                    tokenPos.begin();
                    this.incrCursor();
                    this.appendLiteral(this.read(), null);
                    if (!match('\'')) {
                        throw new LexerException("Unbalanced quote for char literal " + this.buffer.toString(), this);
                    }
                    break loop;
                case '"':
                    tokenPos.begin();
                    this.incrCursor();
                    if (match("\"\"")) {
                        type = TokenType.PARAGRAPH;
                        this.readParagraph();
                    } else {
                        type = TokenType.STRING;
                        this.readString();
                    }
                    break loop;
                case '(':
                    type = TokenType.LPAREN;
                    singlePos = snapshot.take(this);
                    this.incrCursor();
                    break loop;
                case ')':
                    type = TokenType.RPAREN;
                    singlePos = snapshot.take(this);
                    this.incrCursor();
                    break loop;
                case '{':
                    type = TokenType.LSCOPE;
                    singlePos = snapshot.take(this);
                    this.incrCursor();
                    break loop;
                case '}':
                    type = TokenType.RSCOPE;
                    singlePos = snapshot.take(this);
                    this.incrCursor();
                    break loop;
                case '[':
                    type = TokenType.LBRACKET;
                    singlePos = snapshot.take(this);
                    this.incrCursor();
                    break loop;
                case ']':
                    type = TokenType.RBRACKET;
                    singlePos = snapshot.take(this);
                    this.incrCursor();
                    break loop;
                case '@':
                    type = TokenType.AT_SIGN;
                    singlePos = snapshot.take(this);
                    this.incrCursor();
                    break loop;
                case '*':
                    type = TokenType.STAR;
                    singlePos = snapshot.take(this);
                    this.incrCursor();
                    break loop;
                case '.':
                    type = TokenType.DOT;
                    singlePos = snapshot.take(this);
                    this.incrCursor();
                    break loop;
                case ',':
                    type = TokenType.CO;
                    singlePos = snapshot.take(this);
                    this.incrCursor();
                    break loop;
                case ';':
                    type = TokenType.SECO;
                    singlePos = snapshot.take(this);
                    this.incrCursor();
                    break loop;
                default:
                    if (Character.isJavaIdentifierStart(this.peekPoint())) {
                        type = TokenType.IDENTIFIER;
                        tokenPos.begin();
                        this.appendCodePoint();
                        this.incrCursor();
                        this.readIdentifier();
                        break loop;
                    }
                    this.incrCursor();
                    break;
            }
        }

        if (type == null && !this.canRead()) {
            return Token.END_OF_INPUT;
        }

        if (type == null) {
            throw new LexerException("Unknown token found", this); // shouldn't really happen unrecognized tokens are just skipped
        }

        if (tokenPos.isInProgress()) {
            tokenPos.end();
        }

        if (CharSequenceToken.TYPES.contains(type)) {
            String value = this.readBuffer();
            if (type == TokenType.IDENTIFIER) {
                type = TokenType.fromValue(value, type);
            }

            TokenCapture record = tokenPos.fetch();
            AbsolutePos startPos = record.start();
            AbsolutePos endPos = record.end();
            return new CharSequenceToken(type, value, startPos.row(), startPos.column(), endPos.column(), startPos.cursor(), endPos.cursor());
        }

        if (CharSequenceBlockToken.TYPES.contains(type)) {
            TokenCapture record = tokenPos.fetch();
            AbsolutePos startPos = record.start();
            AbsolutePos endPos = record.end();
            return new CharSequenceBlockToken(type, this.readLineBuffer(), startPos.row(), endPos.row(), startPos.column(), endPos.column(), startPos.cursor(), endPos.cursor());
        }

        if (singlePos == null) {
            throw new LexerException("Unknown position for " + type, this);
        }

        return new CharToken(type, type.value.charAt(0), singlePos.row(), singlePos.column(), singlePos.cursor(), singlePos.cursor() + this.charSize);
    }

    private boolean isBlank(String line) {
        int i;
        int size = line.length();
        for (i = 0; i < size && isSpace(line.charAt(i)); i++) {
        }
        return i == size;
    }

    private String stripTrailingSpace(String line) {
        int i;
        int size = line.length();
        for (i = size - 1; i >= 0 && isSpace(line.charAt(i)); i--) {
        }
        return line.substring(0, i + 1);
    }
}
