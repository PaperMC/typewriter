package io.papermc.typewriter.parser.exception;

import io.papermc.typewriter.parser.UnicodeTranslator;

public class LexerException extends ReaderException {

    private final int position;
    private final int line;

    public LexerException(String message, UnicodeTranslator translator) {
        this(message, translator, 0);
    }

    public LexerException(String message, UnicodeTranslator translator, int horizontalOffset) {
        super(message);
        this.position = translator.getColumn() + horizontalOffset;
        this.line = translator.getRow();
    }

    @Override
    public String getMessage() {
        StringBuilder message = new StringBuilder(super.getMessage());
        message.append(" near/at position ").append(this.position);
        message.append(" in line ").append(this.line);
        if (this.file != null) {
            message.append(" from class ").append(this.file.mainClass().canonicalName());
        }

        return message.toString();
    }
}
