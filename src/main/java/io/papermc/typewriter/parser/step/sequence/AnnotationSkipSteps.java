package io.papermc.typewriter.parser.step.sequence;

import io.papermc.typewriter.parser.ClosureAdvanceResult;
import io.papermc.typewriter.parser.LineParser;
import io.papermc.typewriter.parser.ParserException;
import io.papermc.typewriter.parser.ProtoTypeName;
import io.papermc.typewriter.parser.StringReader;
import io.papermc.typewriter.parser.closure.Closure;
import io.papermc.typewriter.parser.closure.ClosureType;
import io.papermc.typewriter.parser.step.IterativeStep;
import io.papermc.typewriter.parser.step.StepHolder;
import io.papermc.typewriter.utils.Formatting;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// start once "@" is detected unless commented
// order is: skipPartName -> checkAnnotationName -> checkOpenParenthesis (-> skipParentheses)
public final class AnnotationSkipSteps implements StepHolder {

    public static boolean canStart(StringReader line) {
        if (line.peek() == '@') {
            line.skip();
            return true;
        }
        return false;
    }

    private final IterativeStep skipParenthesesStep = this.repeatStep(this::skipParentheses);

    private @MonotonicNonNull ProtoTypeName name;
    private Closure parenthesisClosure;

    public boolean skipPartName(StringReader line, LineParser parser) {
        boolean checkStartId = this.name == null || this.name.shouldCheckStartIdentifier();

        if (!checkStartId) { // this part is not in the import steps since import always need a semicolon at their end so it's easier to parse them
            if (!parser.trySkipCommentOrWhitespaceUntil(line, ProtoTypeName.IDENTIFIER_SEPARATOR)) { // expect a dot for multi line annotation when the previous line doesn't end by a dot itself
                return false;
            }
        } else {
            parser.skipCommentOrWhitespace(line);
        }
        if (!line.canRead()) {
            return true;
        }

        this.name = line.getPartNameUntil('(', parser::skipCommentOrWhitespace, this.name);

        if (parser.peekSingleLineComment(line)) {
            // ignore single line comment at the end and allow the name to continue
            line.setCursor(line.getTotalLength());
        }
        return !line.canRead();
    }

    private static final List<ClosureType> IGNORE_NESTED_CLOSURES;
    static {
        List<ClosureType> types = new ArrayList<>(ClosureType.LEAFS.size());
        types.addAll(ClosureType.LEAFS);
        types.remove(ClosureType.COMMENT); // comment will be skipped separately to simplify the iteration
        types.add(ClosureType.PARENTHESIS); // skip nested annotation too

        IGNORE_NESTED_CLOSURES = Collections.unmodifiableList(types);
    }

    public boolean skipParentheses(StringReader line, LineParser parser) {
        while (true) {
            if (parser.skipCommentOrWhitespace(line)) {
                break;
            }

            if (parser.getNearestClosure() == null || !ClosureType.LEAFS.contains(parser.getNearestClosure().getType())) { // peekSingleLineComment doesn't check closure
                if (parser.peekSingleLineComment(line)) {
                    line.setCursor(line.getTotalLength());
                    break;
                }
            }

            ClosureAdvanceResult result = parser.tryAdvanceEndClosure(this.parenthesisClosure, line);
            if (result == ClosureAdvanceResult.CHANGED) {
                return false;
            }
            if (!line.canRead()) {
                break;
            }
            if (result == ClosureAdvanceResult.IGNORED && !parser.trySkipNestedClosures(this.parenthesisClosure, line, IGNORE_NESTED_CLOSURES)) {
                line.skip();
            }
        }

        return true; // parenthesis on another line?
    }

    public void checkAnnotationName(StringReader line, LineParser parser) {
        String name = this.name.getFinalName();

        if (name.isEmpty()) {
            throw new ParserException("Invalid java source, annotation name is empty", line);
        }

        if (name.equals("interface")) {
            // skip this one: annotation definition (@interface)
            // note: this can't be skipped before (i.e. in canStart) since space/comments are allowed between '@' and 'interface'
            parser.getSteps().clearRemaining();
            return;
        }

        if (!Formatting.isValidName(name)) { // keyword are checked after to simplify things
            throw new ParserException("Invalid java source, annotation name contains a reserved keyword or a syntax error", line);
        }
    }

    public boolean checkOpenParenthesis(StringReader line, LineParser parser) {
        parser.skipCommentOrWhitespace(line); // since skipPartName fail fast this is needed for space between the typeName and the parenthesis
        if (!line.canRead()) {
            return true;
        }

        if (parser.tryAdvanceStartClosure(ClosureType.PARENTHESIS, line)) { // open parenthesis?
            this.parenthesisClosure = parser.getNearestClosure();
            parser.getSteps().executeNext(this.skipParenthesesStep);
        }
        return false;
    }

    @Override
    public IterativeStep[] initialSteps() {
        return new IterativeStep[] {
            this.repeatStep(this::skipPartName),
            this.onceStep(this::checkAnnotationName),
            this.repeatStep(this::checkOpenParenthesis),
        };
    }
}
