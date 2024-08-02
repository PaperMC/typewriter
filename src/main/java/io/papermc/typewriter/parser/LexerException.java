package io.papermc.typewriter.parser;

import io.papermc.typewriter.SourceFile;
import org.jetbrains.annotations.Contract;

public class LexerException extends IllegalStateException {

    private final UnicodeTranslates reader;
    private SourceFile file;
    private int lineCount;

    public LexerException(String message, UnicodeTranslates reader) {
        super(message);
        this.reader = reader;
    }

    @Contract(value = "_, _ -> this", mutates = "this")
    public LexerException withAdditionalContext(SourceFile file, int lineCount) {
        this.file = file;
        this.lineCount = lineCount;
        return this;
    }

    @Override
    public String getMessage() {
        StringBuilder message = new StringBuilder(super.getMessage());
        message.append(" near/at position ").append(this.reader.getCursor());
        if (this.reader.canRead()) {
            message.append(" (").append(this.reader.peek()).append(')');
        }
        if (this.file != null) {
            message.append(" in line ").append(this.lineCount).append(" from class ").append(this.file.mainClass().canonicalName());
        }

        return message.toString();
    }
}
