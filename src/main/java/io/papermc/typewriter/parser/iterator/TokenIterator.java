package io.papermc.typewriter.parser.iterator;

import io.papermc.typewriter.parser.Tokenizer;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class TokenIterator implements Iterator<Token> {

    protected final Tokenizer tokenizer;

    private Token nextToken;

    public TokenIterator(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    @Override
    public boolean hasNext() {
        if (this.nextToken == null) {
            this.nextToken = this.tokenizer.readToken();
        }

        return this.nextToken.type() != TokenType.EOI;
    }

    @Override
    public Token next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }

        Token cached = this.nextToken; // next token is always defined after hasNext
        this.nextToken = null;
        return cached;
    }
}
