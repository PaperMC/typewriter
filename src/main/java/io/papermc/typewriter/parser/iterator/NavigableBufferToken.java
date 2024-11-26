package io.papermc.typewriter.parser.iterator;

import io.papermc.typewriter.parser.Tokenizer;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class NavigableBufferToken {

    private final Tokenizer tokenizer;
    private final List<Token> buffer;
    private final int size;
    private int index;

    public NavigableBufferToken(Tokenizer tokenizer, int size) {
        this.tokenizer = tokenizer;
        this.size = size;
        this.buffer = new ArrayList<>(size);
    }

    public boolean hasPrevious() {
        return this.index > 0 && !this.buffer.isEmpty();
    }

    private @Nullable Token getToken(boolean forward) {
        int nextIndex = forward ? this.index : this.index - 1;
        return nextIndex >= this.buffer.size() ? null : this.buffer.get(nextIndex);
    }

    private void updateBuffer(boolean forward, @Nullable Token token) {
        if (token != null && token.type() == TokenType.EOI) {
            return;
        }

        if (this.index >= this.size) {
            if (forward) {
                this.buffer.removeFirst();
                this.index--;
            } else {
                this.buffer.removeLast();
            }
        }

        if (forward && token != null) {
            this.buffer.addLast(token);
        }
    }

    public boolean hasNext() {
        Token nextToken = this.getToken(true);
        if (nextToken == null) {
            nextToken = this.tokenizer.readToken();
            this.updateBuffer(true, nextToken);
        }
        return nextToken.type() != TokenType.EOI;
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

        Token token = this.getToken(true);
        if (token == null) {
            token = this.tokenizer.readToken();
            this.updateBuffer(true, token);
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

        Token token = this.getToken(false);
        if (token == null) {
            this.updateBuffer(false, null);
        }
        if (updateCursor) {
            this.index--;
        }
        return token;
    }
}
