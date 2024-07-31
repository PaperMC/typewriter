package io.papermc.typewriter.parser;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.context.ImportCollector;
import io.papermc.typewriter.parser.closure.AbstractClosure;
import io.papermc.typewriter.parser.closure.Closure;
import io.papermc.typewriter.parser.closure.ClosureType;
import io.papermc.typewriter.parser.name.ProtoTypeName;
import io.papermc.typewriter.parser.step.StepManager;
import io.papermc.typewriter.parser.step.sequence.AnnotationSkipSteps;
import io.papermc.typewriter.parser.step.sequence.ImportStatementSteps;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.function.IntPredicate;

@ApiStatus.Internal
@DefaultQualifier(NonNull.class)
public class LineParser {

    private final StepManager stepManager = new StepManager(this);

    private @Nullable Closure nearestClosure;

    public @Nullable Closure getNearestClosure() {
        return this.nearestClosure;
    }

    // internal use only or when nearestClosure = null
    // doesn't support leaf closure char escape
    public boolean tryAdvanceStartClosure(ClosureType type, StringReader line) {
        if (line.trySkipString(type.start)) { // closure has been consumed
            @Nullable Closure previousNearestClosure = this.nearestClosure;
            this.nearestClosure = Closure.create(type);
            if (previousNearestClosure != null) {
                if (ClosureType.LEAFS.contains(previousNearestClosure.getType())) {
                    throw new ParserException("Nested closure in a leaf closure is not allowed", line);
                }
                ((AbstractClosure) this.nearestClosure).setParent(previousNearestClosure);
            }
            this.nearestClosure.onStart(line);
            return true;
        }
        return false;
    }

    private boolean isCurrentlyEscaped(StringReader line, int offset) {
        int delimiterCursor = line.getCursor() - offset; // escaped sequence is only a single char
        int cursor = 1;
        while (delimiterCursor >= cursor && line.getString().charAt(delimiterCursor - cursor) == '\\') {
            cursor++;
        }

        int escapedBlock = cursor - 1;
        return (escapedBlock & 1) != 0; // odd size means the char is always escaped
    }

    // for all closure, leaf closure type should use the other similar method after this one if possible
    public ClosureAdvanceResult tryAdvanceEndClosure(Closure closure, StringReader line) {
        Preconditions.checkState(this.nearestClosure != null && this.nearestClosure.hasUpperClosure(closure), "Need to be in an upper closure of " + closure + " to find its end identifier");
        boolean directClosureFound = this.nearestClosure == closure;
        if (!directClosureFound) {
            return ClosureAdvanceResult.IGNORED;
        }

        ClosureType type = closure.getType();
        if (line.trySkipString(type.end)) { // closure has been consumed
            // skip escape closed closure
            if (type.escapableByPreviousChar() && this.isCurrentlyEscaped(line, 1)) {
                return ClosureAdvanceResult.SKIPPED;
            }

            this.nearestClosure.onEnd(line);
            this.nearestClosure = this.nearestClosure.parent();
            return ClosureAdvanceResult.CHANGED;
        }

        return ClosureAdvanceResult.IGNORED;
    }

    // computedTypes list order matters here
    public boolean trySkipNestedClosures(Closure inClosure, StringReader line, List<ClosureType> computedTypes) {
        boolean directClosureFound = this.nearestClosure == inClosure;
        boolean isLeaf = this.nearestClosure != null && ClosureType.LEAFS.contains(this.nearestClosure.getType());
        if (this.nearestClosure != null && !directClosureFound) {
            final ClosureAdvanceResult result;
            if (isLeaf) {
                result = this.tryAdvanceEndLeafClosure(this.nearestClosure.getType(), line);
            } else {
                result = this.tryAdvanceEndClosure(this.nearestClosure, line);
            }
            if (result != ClosureAdvanceResult.IGNORED) {
                return true;
            }
        }

        if (this.nearestClosure == null || !isLeaf) { // leaf take the priority and doesn't allow any other nested type
            for (ClosureType type : computedTypes) {
                if (this.tryAdvanceStartClosure(type, line)) {
                    return true;
                }
            }
        }
        return false;
    }

    public ClosureAdvanceResult tryAdvanceEndLeafClosure(ClosureType type, StringReader line) {
        Preconditions.checkArgument(ClosureType.LEAFS.contains(type), "Only leaf closure can be advanced using its type only, for other types use the closure equivalent method to take in account nested closures");
        Preconditions.checkState(this.nearestClosure != null && this.nearestClosure.getType() == type, "Need a direct upper closure of " + type);

        if (line.trySkipString(type.end)) { // closure has been consumed
            // skip escape closed closure
            if (type.escapableByPreviousChar() && this.isCurrentlyEscaped(line, 1)) {
                return ClosureAdvanceResult.SKIPPED;
            }

            this.nearestClosure.onEnd(line);
            this.nearestClosure = this.nearestClosure.parent();
            return ClosureAdvanceResult.CHANGED;
        }
        return ClosureAdvanceResult.IGNORED;
    }

    // generic usage that check other leaf closure
    private boolean skipLeafClosure(ClosureType type, StringReader line) {
        final boolean isInClosure;
        if (this.nearestClosure != null) {
            isInClosure = this.nearestClosure.getType() == type;
            if (!isInClosure && ClosureType.LEAFS.contains(this.nearestClosure.getType())) {
                // don't try to advance the pointer when the nearest closure is another
                // leaf closure (leaf closure doesn't allow nested type)
                return false;
            }
        } else {
            isInClosure = false;
        }

        int previousCursor = line.getCursor();
        if (isInClosure ||
            this.tryAdvanceStartClosure(type, line)) { // open closure?
            ClosureAdvanceResult result;
            while ((result = this.tryAdvanceEndLeafClosure(type, line)) != ClosureAdvanceResult.CHANGED && line.canRead()) { // closed closure?
                if (result == ClosureAdvanceResult.IGNORED) {
                    line.skip();
                }
            }
            return line.getCursor() > previousCursor;
        }
        return false;
    }

    public boolean skipComment(StringReader line) {
        return this.skipLeafClosure(ClosureType.COMMENT, line);
    }

    public boolean skipCommentOrWhitespace(StringReader line) {
        boolean skipped = false;
        while (this.skipComment(line) || line.skipWhitespace() > 0 || this.skipUnicodeEscape(line, Lexer::isWhitespace)) {
            skipped = true;
        }
        return skipped;
    }

    // unicode aware version of StringReader#skipWhitespace
    public int skipAllWhitespace(StringReader line) {
        int skipped = 0;
        while (true) {
            int actualCount = skipped;
            skipped += line.skipWhitespace();
            if (this.skipUnicodeEscape(line, Lexer::isWhitespace)) {
                skipped++;
            }
            if (actualCount == skipped) {
                // means nothing has been skipped in this turn
                break;
            }
        }
        return skipped;
    }

    // the compiler allow the whole program to be written using unicode escape (IDE might complain tho) to simplify things only
    // handle whitespace escape for now
    // return codePoint << 32 | escapeSize
    private @Nullable Long parseUnicodeEscape(StringReader line) {
        if (!line.canRead(2)) { // prefix size is \\u
            return null;
        }

        if (line.peek() == '\\') {
            if (this.isCurrentlyEscaped(line, 0)) { // check if backslash is escaped itself
                return null;
            }

            int prefixSize = 1;
            while (line.canRead(prefixSize + 1) && line.peek(prefixSize) == 'u') { // match as many unicode marker ('u') as possible
                prefixSize++;
            }

            if (prefixSize > 1) { // found unicode sequence -> parse 4 hexadecimal digits
                if (!line.canRead(prefixSize + 4)) {
                    throw new ParserException("Invalid java source, found a malformed unicode escape sequence: missing/incomplete code point value", line);
                }

                int codePoint = 0;
                for (int i = 0; i < 4; i++) {
                    char c = line.peek(prefixSize + i);
                    int digit = Character.digit(c, 16);
                    if (digit == -1) {
                        String read = line.getRemaining().substring(0, prefixSize + i);
                        throw new ParserException("Invalid java source, found a malformed unicode escape sequence: invalid hexadecimal digit '%c' after %s".formatted(c, read), line);
                    }

                    codePoint = codePoint << 4 | digit;
                }

                return ((long) codePoint << 32) | (prefixSize + 4);
            }
        }
        return null;
    }

    private boolean skipUnicodeEscape(StringReader line, IntPredicate canSkip) {
        @Nullable Long unicodeEscape = parseUnicodeEscape(line);
        if (unicodeEscape == null) {
            return false;
        }

        int codePoint = (int) (unicodeEscape.longValue() >>> 32);
        int skipped = unicodeEscape.intValue();
        if (!Character.isSurrogate((char) codePoint)) { // safe cast char is on 16 bits like UTF-16 escape sequence
            if (canSkip.test(codePoint)) {
                // regular unicode escape for non-astral code point (plane 0)
                line.setCursor(line.getCursor() + skipped);
                return true;
            } else {
                return false;
            }
        }

        char hi = (char) codePoint;
        if (!Character.isHighSurrogate(hi)) {
            throw new ParserException("Invalid java source, found a low surrogate (\\u%04X) before its sibling".formatted(codePoint), line);
        }

        @Nullable Long nextUnicodeEscape;
        int previousCursor = line.getCursor();
        line.setCursor(line.getCursor() + skipped);
        try {
            nextUnicodeEscape = this.parseUnicodeEscape(line);
        } finally {
            line.setCursor(previousCursor);
        }

        if (nextUnicodeEscape == null) {
            throw new ParserException("Invalid java source, found a high surrogate (\\u%04X) without its sibling".formatted(codePoint), line);
        }

        char lo = (char) (nextUnicodeEscape.longValue() >>> 32);
        if (!Character.isLowSurrogate(lo)) {
            throw new ParserException("Invalid java source, found a malformed surrogate pair: low surrogate is invalid (\\u%04X)".formatted((int) lo), line);
        }

        if (canSkip.test(Character.toCodePoint(hi, lo))) {
            skipped += nextUnicodeEscape.intValue();
            line.setCursor(line.getCursor() + skipped);
            return true;
        }
        return false;
    }

    public boolean trySkipCommentOrWhitespaceUntil(StringReader line, char terminator) {
        int previousCursor = line.getCursor();
        boolean skipped = this.skipCommentOrWhitespace(line);
        if (skipped && line.canRead() && line.peek() != terminator) {
            line.setCursor(previousCursor);
            skipped = false;
        }

        return skipped;
    }

    // note: always check single line comment AFTER multi line comment unless exception
    public boolean peekSingleLineComment(StringReader line) {
        return line.canRead(2) && line.peek() == '/' && line.peek(1) == '/';
    }

    public void getTypeNameUntil(StringReader line, char terminator, ProtoTypeName type) {
        while (line.canRead()) {
            int code = line.getString().codePointAt(line.getCursor());
            if (code == terminator) {
                type.checkIdentifier(0);
                break;
            }

            int read = type.tryAdvance(code, line);
            if (read == -1) {
                break;
            }

            line.setCursor(line.getCursor() + read);
        }
    }

    public boolean consumeImports(StringReader line, ImportCollector collector) {
        while (line.canRead()) {
            this.stepManager.runSteps(line);

            if (!line.canRead()) {
                break;
            }

            if (this.skipCommentOrWhitespace(line)) {
                continue;
            }

            if (this.peekSingleLineComment(line)) {
                // check single line comment only after multi line to avoid ignoring the end of multi line comment starting with // on the newline
                break;
            }

            // not commented
            char c = line.peek();
            if (AnnotationSkipSteps.canStart(line)) { // handle annotation with param to avoid open curly bracket that occur in array argument
                this.stepManager.enqueue(new AnnotationSkipSteps());
                continue;
            } else if (c == '{') {
                return true;
            } else if (ImportStatementSteps.canStart(line)) {
                this.stepManager.enqueue(new ImportStatementSteps(collector));
                continue;
            }

            line.skip();
        }
        return false;
    }

    public StepManager getSteps() {
        return this.stepManager;
    }
}
