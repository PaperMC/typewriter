package io.papermc.typewriter.parser;

import io.papermc.typewriter.parser.exception.ParserException;
import io.papermc.typewriter.parser.iterator.NavigableToken;
import io.papermc.typewriter.parser.token.CharSequenceToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;
import javax.lang.model.SourceVersion;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class SequenceTokens {

    private final NavigableToken iterator;
    private final Set<TokenType> ignoredTokens;
    private final Queue<TokenTask> expectedTokens = new ArrayDeque<>();
    private TokenTask failedTask;

    public static class TokenTask {
        private final TokenAction action;
        private boolean optional;
        private boolean repeatable;
        private @Nullable Consumer<Token> firstToken;

        public TokenTask(TokenAction action) {
            this.action = action;
        }

        public TokenTask asRepeatable() {
            return this.asRepeatable(null);
        }

        public TokenTask asRepeatable(@Nullable Consumer<Token> firstToken) {
            this.repeatable = true;
            this.firstToken = firstToken;
            return this;
        }

        public TokenTask asOptional() {
            this.optional = true;
            return this;
        }

        private TokenTask validate() {
            if (this.optional && this.repeatable) {
                throw new UnsupportedOperationException("Cannot define a task as optional and repeatable!");
            }
            return this;
        }

        public FailureException createFailure(String message, Token token) {
            return new FailureException(message, token);
        }

        public class FailureException extends ParserException {

            public FailureException(String message, Token token) {
                super(message, token);
            }

            @Override
            public String getMessage() {
                return "%s during execution of task: %s".formatted(super.getMessage(), TokenTask.this.action);
            }
        }
    }

    @FunctionalInterface
    public interface TaskConfigurator extends UnaryOperator<TokenTask> {

        default SequenceTokens.TokenTask bind(TokenAction action) {
            return this.apply(new TokenTask(action)).validate();
        }

        static TaskConfigurator empty() {
            return t -> t;
        }
    }

    private SequenceTokens(SequenceTokens from) {
        this.iterator = from.iterator;
        this.ignoredTokens = from.ignoredTokens;
    }

    private SequenceTokens(Lexer lex, Set<TokenType> ignoredTokens) {
        this.iterator = new NavigableToken(lex, false);
        this.ignoredTokens = ignoredTokens;
    }

    public static SequenceTokens wrap(Lexer lex) {
        return new SequenceTokens(lex, EnumSet.noneOf(TokenType.class));
    }

    public static SequenceTokens wrap(Lexer lex, Set<TokenType> ignoredTokens) {
        return new SequenceTokens(lex, EnumSet.copyOf(ignoredTokens));
    }

    public SequenceTokens map(TokenType type, Consumer<Token> callback) {
        return this.map(type, callback, TaskConfigurator.empty());
    }

    public SequenceTokens map(TokenType type, Consumer<Token> callback, TaskConfigurator parameters) {
        return this.map(type, callback, null, parameters);
    }

    public SequenceTokens map(TokenType type, Consumer<Token> callback, @Nullable Consumer<SequenceTokens> subAction, TaskConfigurator parameters) {
        if (this.ignoredTokens.contains(type)) {
            throw new UnsupportedOperationException("Cannot attempt to read an ignored token type: " + type.name());
        }

        this.expectedTokens.offer(parameters.bind(new CallableAction(type, callback, subAction)));
        return this;
    }

    public SequenceTokens mapMulti(Set<TokenType> types, Consumer<Token> callback) {
        return this.mapMulti(types, callback, TaskConfigurator.empty());
    }

    public SequenceTokens mapMulti(Set<TokenType> types, Consumer<Token> callback, TaskConfigurator parameters) {
        for (TokenType type : types) {
            if (this.ignoredTokens.contains(type)) {
                throw new UnsupportedOperationException("Cannot attempt to read an ignored token type: " + type.name());
            }
        }

        this.expectedTokens.offer(parameters.bind(new CallableActionMulti(types, callback)));
        return this;
    }

    public SequenceTokens mapQualifiedName(Consumer<CharSequenceToken> nameCallback, Consumer<Token> dotCallback, @Nullable Consumer<SequenceTokens> partialAction) {
        return this.mapQualifiedName(nameCallback, dotCallback, Collections.emptySet(), partialAction);
    }

    public SequenceTokens mapQualifiedName(Consumer<CharSequenceToken> nameCallback, Consumer<Token> dotCallback, Set<TokenType> ignoreTokens, @Nullable Consumer<SequenceTokens> partialAction) {
        if (this.ignoredTokens.contains(TokenType.IDENTIFIER) || this.ignoredTokens.contains(TokenType.DOT)) {
            throw new UnsupportedOperationException("Cannot attempt to read an already ignored token type: " + this.ignoredTokens);
        }

        this.expectedTokens.offer(TaskConfigurator.empty().bind(new CallableQualifiedNameAction(nameCallback, dotCallback, ignoreTokens, partialAction)));
        return this;
    }

    public SequenceTokens group(Consumer<SequenceTokens> subAction, TaskConfigurator parameters) {
        this.expectedTokens.offer(parameters.bind(new SubAction(subAction)));
        return this;
    }

    public SequenceTokens skip(TokenType type) {
        return this.skip(type, TaskConfigurator.empty());
    }

    public SequenceTokens skip(TokenType type, TaskConfigurator parameters) {
        return this.skip(type, null, parameters);
    }

    public SequenceTokens skip(TokenType type, @Nullable Consumer<SequenceTokens> subAction) {
        return this.skip(type, subAction, TaskConfigurator.empty());
    }

    public SequenceTokens skip(TokenType type, @Nullable Consumer<SequenceTokens> subAction, TaskConfigurator parameters) {
        if (this.ignoredTokens.contains(type)) {
            throw new UnsupportedOperationException("Cannot attempt to skip an already ignored token type: " + type.name());
        }

        this.expectedTokens.offer(parameters.bind(new SkipAction(type, subAction)));
        return this;
    }

    public SequenceTokens skipQualifiedName() {
        return this.skipQualifiedName(Collections.emptySet());
    }

    public SequenceTokens skipQualifiedName(Set<TokenType> ignoreTokens) {
        return this.skipQualifiedName(ignoreTokens, null);
    }

    public SequenceTokens skipQualifiedName(@Nullable Consumer<SequenceTokens> partialAction) {
        return this.skipQualifiedName(Collections.emptySet(), partialAction);
    }

    public SequenceTokens skipQualifiedName(Set<TokenType> transparentTokens, @Nullable Consumer<SequenceTokens> partialAction) {
        if (this.ignoredTokens.contains(TokenType.IDENTIFIER) || this.ignoredTokens.contains(TokenType.DOT)) {
            throw new UnsupportedOperationException("Cannot attempt to skip an already ignored token type: " + this.ignoredTokens);
        }
        if (transparentTokens.contains(TokenType.IDENTIFIER) || transparentTokens.contains(TokenType.DOT)) {
            throw new UnsupportedOperationException("Transparent tokens cannot be an identifier or a dot: " + transparentTokens);
        }

        this.expectedTokens.offer(TaskConfigurator.empty().bind(new CallableQualifiedNameAction(name -> {}, dot -> {}, transparentTokens, partialAction)));
        return this;
    }

    public SequenceTokens skipClosure(TokenType open, TokenType close, boolean nested) {
        this.expectedTokens.offer(TaskConfigurator.empty().bind(new SkipClosureAction(open, close, nested)));
        return this;
    }

    public boolean execute() {
        return this.execute(null);
    }

    public boolean executeOrThrow(BiFunction<TokenTask, Token, Exception> failure) {
        return this.execute(failedTask -> {
            throw new RuntimeException(failure.apply(failedTask, this.iterator.peekPrevious()));
        });
    }

    public boolean execute(@Nullable Consumer<TokenTask> failure) {
        if (this.expectedTokens.isEmpty()) {
            throw new IllegalStateException("Expected tokens list is empty");
        }

        try {
            TokenTask failedTask = null;
            while (!this.expectedTokens.isEmpty() && this.iterator.hasNext()) {
                Token token = this.iterator.next();
                if (this.ignoredTokens.contains(token.type())) {
                    continue;
                }

                TokenTask task = this.expectedTokens.peek();

                boolean alreadyRan = false;
                if (task.repeatable) {
                    if (task.firstToken != null) {
                        task.firstToken.accept(token);
                        task.firstToken = null;
                    } else {
                        alreadyRan = true;
                    }
                }

                boolean success = task.action.execute(token, this, this.iterator);
                if (!task.repeatable || (!success && alreadyRan)) {
                    this.expectedTokens.poll();
                    //this.expectedTokens.remove(task);
                }

                if (!success) {
                    if (task.optional || alreadyRan) { // at least one run for repeat
                        this.iterator.previous();
                    } else {
                        failedTask = task;
                        break;
                    }
                }
            }

            boolean done = this.expectedTokens.isEmpty() && failedTask == null;
            if (!done) {
                this.failedTask = failedTask != null ? failedTask : this.expectedTokens.peek();
            }
            return done;
        } finally {
            if (this.failedTask != null && failure != null) {
                failure.accept(this.failedTask);
            }
            this.failedTask = null;
        }
    }

    private boolean executeSub(SequenceTokens from) {
        return this.execute((failedTask -> from.failedTask = failedTask));
    }

    public interface TokenAction {

        boolean execute(Token token, SequenceTokens executor, NavigableToken expectedTokens);
    }

    private record SubAction(Consumer<SequenceTokens> subAction) implements TokenAction {

        @Override
        public boolean execute(Token token, SequenceTokens executor, NavigableToken expectedTokens) {
            expectedTokens.previous(); // get the initial token into the next pipe
            SequenceTokens sequence = new SequenceTokens(executor);
            this.subAction.accept(sequence);
            return sequence.executeSub(executor);
        }
    }

    private record SkipAction(TokenType type, @Nullable Consumer<SequenceTokens> subAction) implements TokenAction {

        @Override
        public boolean execute(Token token, SequenceTokens executor, NavigableToken expectedTokens) {
            boolean foundToken = this.type == token.type();
            if (foundToken && this.subAction != null) {
                SequenceTokens sequence = new SequenceTokens(executor);
                this.subAction.accept(sequence);
                return sequence.executeSub(executor);
            }
            return foundToken;
        }
    }

    private record SkipClosureAction(TokenType open, TokenType close, boolean nested) implements TokenAction {

        @Override
        public boolean execute(Token token, SequenceTokens executor, NavigableToken expectedTokens) {
            if (token.type() != this.open) {
                return false;
            }

            int depth = 1;
            while (expectedTokens.hasNext()) {
                Token currentToken = expectedTokens.next();
                if (executor.ignoredTokens.contains(currentToken.type())) {
                    continue;
                }

                if (this.nested) {
                    if (currentToken.type() == this.open) {
                        depth++;
                    }
                }

                if (currentToken.type() == this.close) {
                    depth--;
                }

                if (depth == 0) {
                    return true;
                }
            }
            return false;
        }
    }

    private record CallableAction(TokenType type, Consumer<Token> callback, @Nullable Consumer<SequenceTokens> subAction) implements TokenAction {

        @Override
        public boolean execute(Token token, SequenceTokens executor, NavigableToken expectedTokens) {
            if (this.type == token.type()) {
                this.callback.accept(token);
                if (this.subAction != null) {
                    SequenceTokens sequence = new SequenceTokens(executor);
                    this.subAction.accept(sequence);
                    return sequence.executeSub(executor);
                }
                return true;
            }
            return false;
        }
    }

    private record CallableActionMulti(Set<TokenType> types, Consumer<Token> callback) implements TokenAction {

        @Override
        public boolean execute(Token token, SequenceTokens executor, NavigableToken expectedTokens) {
            if (this.types.contains(token.type())) {
                this.callback.accept(token);
                return true;
            }
            return false;
        }
    }

    private record CallableQualifiedNameAction(Consumer<CharSequenceToken> nameCallback, Consumer<Token> dotCallback, Set<TokenType> transparentTokens, @Nullable Consumer<SequenceTokens> partialAction) implements TokenAction {

        @Override
        public boolean execute(Token token, SequenceTokens executor, NavigableToken expectedTokens) {
            if (token.type() != TokenType.IDENTIFIER) {
                return false;
            }

            this.nameCallback.accept((CharSequenceToken) token);

            boolean expectDot = true;
            Token lastToken = token;
            while (expectedTokens.hasNext()) {
                Token currentToken = expectedTokens.next();
                if (executor.ignoredTokens.contains(currentToken.type()) || this.transparentTokens.contains(currentToken.type())) {
                    continue;
                }

                if (currentToken.type() != (expectDot ? TokenType.DOT : TokenType.IDENTIFIER)) {
                    expectedTokens.previous();
                    break;
                }
                lastToken = currentToken;

                if (!expectDot && SourceVersion.isKeyword(((CharSequenceToken) currentToken).value())) { // invalid name
                    return false;
                }

                if (expectDot) {
                    this.dotCallback.accept(currentToken);
                } else {
                    this.nameCallback.accept((CharSequenceToken) currentToken);
                }
                expectDot = !expectDot;
            }

            if (lastToken.type() == TokenType.IDENTIFIER) {
                return true;
            }

            if (this.partialAction != null && lastToken.type() == TokenType.DOT) {
                SequenceTokens sequence = new SequenceTokens(executor);
                this.partialAction.accept(sequence);
                return sequence.executeSub(executor);
            }
            return false;
        }
    }
}
