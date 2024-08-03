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
        StringBuilder message = new StringBuilder(super.getMessage());
        message.append(" near/at position ").append(this.badToken.column());
        message.append(" in line ").append(this.badToken.row());
        message.append(" (token: ").append(this.badToken).append(')');
        if (this.file != null) {
            message.append(" from class ").append(this.file.mainClass().canonicalName());
        }

        return message.toString();
    }
}
