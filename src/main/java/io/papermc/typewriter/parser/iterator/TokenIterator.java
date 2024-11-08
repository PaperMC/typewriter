package io.papermc.typewriter.parser.iterator;

import io.papermc.typewriter.parser.Lexer;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class TokenIterator implements Iterator<Token> {

    protected final Lexer lex;
    private final boolean ignoreEOI;

    private Token cachedToken;

    public TokenIterator(Lexer lex, boolean ignoreEOI) {
        this.lex = lex;
        this.ignoreEOI = ignoreEOI;
    }

    @Override
    public boolean hasNext() {
        boolean canRead = this.lex.canRead();
        if (canRead) {
            if (!this.ignoreEOI) {
                if (this.cachedToken == null) {
                    this.cachedToken = this.lex.readToken();
                }
                return this.cachedToken.type() != TokenType.EOI;
            }
        }

        return canRead;
    }

    @Override
    public Token next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }

        if (!this.ignoreEOI) {
            Token cached = this.cachedToken;
            this.cachedToken = null;
            return cached;
        }

        return this.lex.readToken();
    }
}
