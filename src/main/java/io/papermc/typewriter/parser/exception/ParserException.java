package io.papermc.typewriter.parser.exception;

import io.papermc.typewriter.parser.token.PrintableToken;

public class ParserException extends ReaderException {

    private final PrintableToken badToken;

    public ParserException(String message, PrintableToken badToken) {
        super(message);
        this.badToken = badToken;
    }

    @Override
    public String getMessage() {
        return "%s near/at position %d in line %d (token: %s)".formatted(super.getMessage(), this.badToken.column(), this.badToken.row(), this.badToken);
    }
}
