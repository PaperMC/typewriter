package io.papermc.typewriter.parser;

import io.papermc.typewriter.SourceFile;
import org.jetbrains.annotations.Contract;

public class ParserException extends IllegalStateException {

    private final StringReader reader;
    private SourceFile file;
    private int lineCount;

    public ParserException(String message, StringReader reader) {
        super(message);
        this.reader = reader;
    }

    @Contract(value = "_, _ -> this", mutates = "this")
    public ParserException withAdditionalContext(SourceFile file, int lineCount) {
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
        if (this.file == null) {
            message.append(" (in line: ").append('"').append(this.reader.getString()).append('"').append(')');
        } else {
            message.append(" in line ").append(this.lineCount).append(" from class ").append(this.file.mainClass().canonicalName());
        }

        return message.toString();
    }
}
