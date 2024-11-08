package io.papermc.typewriter.parser.token;

public final class TokenBlockPosition implements TokenRecorder {

    public AbsolutePos startPos;
    public AbsolutePos endPos;

    public void begin(Token token) {
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
        int endPos;
        int endRow = token.row(), endColumn;
        switch (token) {
            case RawToken rawToken -> {
                endPos = rawToken.pos() + 1; // todo check: this is probably wrong with unicode escape
                endColumn = rawToken.column() + 1;
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
        }

        this.endPos = new AbsolutePos(
            endPos,
            endRow,
            endColumn
        );
    }
}
