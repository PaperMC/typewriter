package io.papermc.typewriter.parser.step.sequence;

import io.papermc.typewriter.context.ImportCollector;
import io.papermc.typewriter.context.ImportTypeCollector;
import io.papermc.typewriter.parser.name.ImportTypeName;
import io.papermc.typewriter.parser.LineParser;
import io.papermc.typewriter.parser.StringReader;
import io.papermc.typewriter.parser.step.IterativeStep;
import io.papermc.typewriter.parser.step.StepHolder;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

// start once "import" is detected unless commented
// order is: enforceSpace -> checkStatic (-> enforceSpace) -> getTypeName -> collectImport
public final class ImportStatementSteps implements StepHolder {

    public static boolean canStart(StringReader line) {
        return line.trySkipString("import");
    }

    private static final char STATEMENT_TERMINATOR = ';';

    private final IterativeStep enforceSpaceStep = this.onceStep(this::enforceSpace);

    private final ImportCollector collector;
    private boolean isStatic;
    private @MonotonicNonNull ImportTypeName name;

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
            parser.getSteps().executeNext(this.enforceSpaceStep);
            this.isStatic = true;
        }
        return false;
    }

    public void collectImport(StringReader line, LineParser parser) {
        this.name.throwIfMalformed();
        ((ImportTypeCollector) this.collector).addProtoImport(this.name);
    }

    public boolean getTypeName(StringReader line, LineParser parser) {
        parser.skipCommentOrWhitespace(line);
        if (!line.canRead()) {
            return true;
        }

        if (this.name == null) {
            this.name = new ImportTypeName(parser::skipCommentOrWhitespace, this.isStatic);
        }

        parser.getTypeNameUntil(line, STATEMENT_TERMINATOR, this.name);

        if (line.canRead()) {
            if (parser.peekSingleLineComment(line)) {
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
            this.repeatStep(this::getTypeName),
            this.onceStep(this::collectImport)
        };
    }
}
