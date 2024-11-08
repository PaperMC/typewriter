package io.papermc.typewriter.parser.iterator;

import io.papermc.typewriter.parser.Lexer;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class NavigableToken {

    private final Lexer lex;
    private final boolean ignoreEOI;
    private final List<Token> tokens;
    private int index = 0;

    public NavigableToken(Lexer lex, boolean ignoreEOI) {
        this.lex = lex;
        this.ignoreEOI = ignoreEOI;
        this.tokens = new ArrayList<>();
    }

    private Token getToken(int index) {
        return index >= this.tokens.size() ? null : this.tokens.get(index);
    }

    private void setToken(int index, Token token) {
        if (index >= this.tokens.size()) {
            this.tokens.add(token);
        } else {
            this.tokens.set(index, token);
        }
    }

    public boolean hasPrevious() {
        return this.index > 0;
    }

    public boolean hasNext() {
        boolean canRead = this.lex.canRead();
        if (canRead) {
            if (!this.ignoreEOI) {
                Token nextToken = this.getToken(this.index);
                if (nextToken == null) {
                    nextToken = this.lex.readToken();
                    this.setToken(this.index, nextToken);
                }
                return nextToken.type() != TokenType.EOI;
            }
        }

        return canRead;
    }

    public Token next() {
        return this.next0(true);
    }

    public Token peekNext() {
        return this.next0(false);
    }

    public Token previous() {
        return this.previous0(true);
    }

    public Token peekPrevious() {
        return this.previous0(false);
    }

    private Token next0(boolean updateCursor) {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }

        Token token = this.getToken(this.index);
        if (token == null) {
            token = this.lex.readToken();
            this.setToken(this.index, token);
        }
        if (updateCursor) {
            this.index++;
        }
        return token;
    }

    private Token previous0(boolean updateCursor) {
        if (!this.hasPrevious()) {
            throw new NoSuchElementException();
        }

        Token token = this.getToken(this.index - 1);
        if (updateCursor) {
            this.index--;
        }
        return token;
    }
}
