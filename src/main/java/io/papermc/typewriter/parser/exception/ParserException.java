package io.papermc.typewriter.parser.exception;

import io.papermc.typewriter.parser.token.Token;

public class ParserException extends ReaderException {

    private final Token badToken;

    public ParserException(String message, Token badToken) {
        super(message);
        this.badToken = badToken;
    }

    @Override
    public String getMessage() {
        return "%s near/at position %d in line %d (token: %s)".formatted(super.getMessage(), this.badToken.column(), this.badToken.row(), this.badToken);
    }
}
