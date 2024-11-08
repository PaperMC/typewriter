package io.papermc.typewriter.parser.token;

public sealed interface Token permits CharToken, CharSequenceToken, CharSequenceBlockToken, Token.EndOfInput {

    record EndOfInput() implements Token {
        @Override
        public TokenType type() {
            return TokenType.EOI;
        }

        @Override
        public Object value() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int row() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int column() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int pos() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "Token[type=%s]".formatted(this.type());
        }
    }

    Token END_OF_INPUT = new EndOfInput();

    TokenType type();

    Object value();

    int row();

    int column();

    int pos();
}
