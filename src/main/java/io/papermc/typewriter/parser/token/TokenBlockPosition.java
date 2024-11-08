package io.papermc.typewriter.parser.token;

public final class TokenBlockPosition implements TokenRecorder {

    public AbsolutePos startPos;
    public AbsolutePos endPos;

    public void begin(Token token) {
        if (token.type() == TokenType.EOI) {
            throw new UnsupportedOperationException("Cannot track end of input token");
        }
        if (this.startPos != null) {
            throw new UnsupportedOperationException("Cannot begin a token position twice");
        }
        this.startPos = new AbsolutePos(
            token.pos(),
            token.row(),
            token.column()
        );
    }

    public void end(Token token) {
        if (token.type() == TokenType.EOI) {
            throw new UnsupportedOperationException("Cannot track end of input token");
        }

        int endPos;
        int endRow = token.row(), endColumn;
        switch (token) {
            case CharToken charToken -> {
                endPos = charToken.endPos();
                endColumn = charToken.column() + 1;
            }
            case CharSequenceToken charSequenceToken -> {
                endPos = charSequenceToken.endPos();
                endColumn = charSequenceToken.endColumn();
            }
            case CharSequenceBlockToken charSequenceBlockToken -> {
                endPos = charSequenceBlockToken.endPos();
                endRow = charSequenceBlockToken.endRow();
                endColumn = charSequenceBlockToken.endColumn();
            }
            default -> throw new IllegalStateException("Unexpected value: " + token);
        }

        this.endPos = new AbsolutePos(
            endPos,
            endRow,
            endColumn
        );
    }
}
