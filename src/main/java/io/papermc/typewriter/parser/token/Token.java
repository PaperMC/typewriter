package io.papermc.typewriter.parser.token;

public sealed interface Token permits PrintableToken, Token.EndOfInput {

    record EndOfInput() implements Token {
        @Override
        public TokenType type() {
            return TokenType.EOI;
        }

        @Override
        public String toString() {
            return "Token[type=%s]".formatted(this.type());
        }
    }

    Token END_OF_INPUT = new EndOfInput();

    TokenType type();
}
