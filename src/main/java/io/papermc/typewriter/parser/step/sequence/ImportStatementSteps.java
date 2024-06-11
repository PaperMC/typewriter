package io.papermc.typewriter.parser.step.sequence;

import io.papermc.typewriter.context.ImportCollector;
import io.papermc.typewriter.parser.LineParser;
import io.papermc.typewriter.parser.ParserException;
import io.papermc.typewriter.parser.ProtoTypeName;
import io.papermc.typewriter.parser.StringReader;
import io.papermc.typewriter.parser.step.IterativeStep;
import io.papermc.typewriter.parser.step.StepHolder;
import io.papermc.typewriter.utils.Formatting;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

// start once "import" is detected unless commented
// order is: enforceSpace -> checkStatic (-> enforceSpace) -> getPartName (-> skipUntilSemicolonAfterStar) -> collectImport
public final class ImportStatementSteps implements StepHolder {

    public static boolean canStart(StringReader line) {
        return line.trySkipString("import");
    }

    private static final char IMPORT_ON_DEMAND_IDENTIFIER = '*';
    private static final char STATEMENT_TERMINATOR = ';';

    private final IterativeStep enforceSpaceStep = this.onceStep(this::enforceSpace);
    private final IterativeStep skipUntilSemicolonAfterStarStep = this.repeatStep(this::skipUntilSemicolonAfterStar);

    private final ImportCollector collector;
    private boolean isStatic;
    private @MonotonicNonNull ProtoTypeName name;

    public ImportStatementSteps(ImportCollector collector) {
        this.collector = collector;
    }

    public void enforceSpace(StringReader line, LineParser parser) {
        int spaceSkipped = line.skipWhitespace();

        boolean filledGap = false;
        filledGap |= parser.skipComment(line);

        boolean hasSingleLineComment = parser.peekSingleLineComment(line);
        filledGap |= hasSingleLineComment;

        if (hasSingleLineComment) {
            // ignore single line comment at the end of import/static
            line.setCursor(line.getTotalLength());
        }

        if (!filledGap && spaceSkipped == 0) {
            // expect at least one space between import, static and type name unless a comment is here to fill the gap
            parser.getSteps().clearRemaining();
        }
    }

    public boolean checkStatic(StringReader line, LineParser parser) {
        parser.skipCommentOrWhitespace(line);
        if (!line.canRead()) {
            return true;
        }

        if (line.trySkipString("static")) {
            parser.getSteps().addPriority(this.enforceSpaceStep);
            this.isStatic = true;
        }
        return false;
    }

    public void collectImport(StringReader line, LineParser parser) {
        String name = this.name.getFinalName();
        if (name.isEmpty()) {
            throw new ParserException("Invalid java source, import type name is empty", line);
        }
        if (!Formatting.isValidName(name)) { // keyword are checked after to simplify things
            throw new ParserException("Invalid java source, import type name contains a reserved keyword or a syntax error", line);
        }

        if (this.isStatic) {
            this.collector.addStaticImport(name);
        } else {
            this.collector.addImport(name);
        }
    }

    public boolean skipUntilSemicolonAfterStar(StringReader line, LineParser parser) {
        parser.skipCommentOrWhitespace(line);
        if (!line.canRead()) {
            return true;
        }

        if (parser.peekSingleLineComment(line)) {
            line.setCursor(line.getTotalLength());
        }

        if (line.canRead() && line.peek() == STATEMENT_TERMINATOR) {
            this.name.append(IMPORT_ON_DEMAND_IDENTIFIER);
            line.skip();
            return false;
        }

        if (line.canRead()) {
            throw new ParserException("Invalid java source, found a '*' char in the middle of import type name", line);
        }

        return true;
    }

    public boolean getPartName(StringReader line, LineParser parser) {
        parser.skipCommentOrWhitespace(line);
        if (!line.canRead()) {
            return true;
        }

        this.name = line.getPartNameUntil(STATEMENT_TERMINATOR, parser::skipCommentOrWhitespace, this.name);

        if (line.canRead()) {
            if (line.peek() == IMPORT_ON_DEMAND_IDENTIFIER) {
                if (this.name == null || this.name.getLastChar() != ProtoTypeName.IDENTIFIER_SEPARATOR) {
                    throw new ParserException("Invalid java source, expected a dot before a '*' for import on demand", line);
                }

                line.skip();
                parser.getSteps().addPriority(this.skipUntilSemicolonAfterStarStep);
                return false;
            } else if (parser.peekSingleLineComment(line)) {
                // ignore single line comment at the end of the name
                line.setCursor(line.getTotalLength());
            }
        }

        return !line.canRead();
    }

    @Override
    public IterativeStep[] initialSteps() {
        return new IterativeStep[] {
            this.enforceSpaceStep,
            this.repeatStep(this::checkStatic),
            this.repeatStep(this::getPartName),
            this.onceStep(this::collectImport)
        };
    }
}
