package io.papermc.typewriter.parser.token.pos;

import io.papermc.typewriter.parser.Lexer;
import io.papermc.typewriter.parser.token.CharSequenceBlockToken;
import io.papermc.typewriter.parser.token.CharSequenceToken;
import io.papermc.typewriter.parser.token.CharToken;
import io.papermc.typewriter.parser.token.PrintableToken;

public interface TokenRecorder {

    TokenSnapshot.Constant<Lexer> LEXER_INSTANT = new TokenSnapshot.Constant<>() {
        @Override
        public AbsolutePos take(Lexer lex) {
            return new AbsolutePos(
                lex.getCursor(),
                lex.getRow(),
                lex.getColumn()
            );
        }
    };

    TokenSnapshot.Default<PrintableToken> BETWEEN_TOKEN = new TokenSnapshot.Default<>() {
        @Override
        AbsolutePos takeStart(PrintableToken token) {
            return new AbsolutePos(
                token.pos(),
                token.row(),
                token.column()
            );
        }

        @Override
        AbsolutePos takeEnd(PrintableToken token) {
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

            return new AbsolutePos(
                endPos,
                endRow,
                endColumn
            );
        }
    };

    interface Default<T> extends TokenRecorder {

        void begin(T object);

        void end(T object);
    }

    interface Constant extends TokenRecorder {

        void begin();

        void end();
    }

    boolean isInProgress();

    TokenCapture fetch();
}
