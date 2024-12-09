package io.papermc.typewriter.parser.sequence;

import io.papermc.typewriter.parser.Tokenizer;
import io.papermc.typewriter.parser.iterator.NavigableToken;
import io.papermc.typewriter.parser.sequence.hook.HookType;
import io.papermc.typewriter.parser.token.CharSequenceToken;
import io.papermc.typewriter.parser.token.PrintableToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;
import javax.lang.model.SourceVersion;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class SequenceTokens {

    private final NavigableToken iterator;
    private final Set<TokenType> ignoredTokens;
    private final Queue<TokenTask> expectedTokens = new ArrayDeque<>();
    private TokenTask failedTask;

    private SequenceTokens(SequenceTokens from) {
        this.iterator = from.iterator;
        this.ignoredTokens = from.ignoredTokens;
    }

    private SequenceTokens(Tokenizer tokenizer, Set<TokenType> ignoredTokens) {
        this.iterator = new NavigableToken(tokenizer);
        this.ignoredTokens = ignoredTokens;
    }

    private NavigableToken iterator() {
        return this.iterator;
    }

    private TokenTask newTask(TokenAction action, UnaryOperator<TokenTaskBuilder> params) {
        return ((TokenTask.Builder) params.apply(new TokenTask.Builder(action))).build();
    }

    private TokenTask newTask(TokenAction action) {
        return new TokenTask.Builder(action).build();
    }

    public static SequenceTokens wrap(Tokenizer tokenizer) {
        return new SequenceTokens(tokenizer, EnumSet.noneOf(TokenType.class));
    }

    public static SequenceTokens wrap(Tokenizer tokenizer, Set<TokenType> ignoredTokens) {
        return new SequenceTokens(tokenizer, EnumSet.copyOf(ignoredTokens));
    }

    public SequenceTokens map(TokenType type, Consumer<PrintableToken> callback) {
        return this.map(type, callback, UnaryOperator.identity());
    }

    public SequenceTokens map(TokenType type, Consumer<PrintableToken> callback, UnaryOperator<TokenTaskBuilder> parameters) {
        return this.map(type, callback, null, parameters);
    }

    public SequenceTokens map(TokenType type, Consumer<PrintableToken> callback, @Nullable Consumer<SequenceTokens> subAction, UnaryOperator<TokenTaskBuilder> parameters) {
        if (this.ignoredTokens.contains(type)) {
            throw new IllegalStateException("Cannot attempt to read an ignored token type: " + type.name());
        }

        this.expectedTokens.offer(newTask(new CallableAction(token -> token.type() == type, callback, subAction), parameters));
        return this;
    }

    public SequenceTokens map(Predicate<TokenType> types, Consumer<PrintableToken> callback) {
        return this.map(types, callback, UnaryOperator.identity());
    }

    public SequenceTokens map(Predicate<TokenType> types, Consumer<PrintableToken> callback, UnaryOperator<TokenTaskBuilder> parameters) {
        for (TokenType ignoredType : this.ignoredTokens) {
            if (types.test(ignoredType)) {
                throw new IllegalStateException("Cannot attempt to read an ignored token type: " + ignoredType.name());
            }
        }

        this.expectedTokens.offer(newTask(new CallableAction(token -> types.test(token.type()), callback, null), parameters));
        return this;
    }

    public SequenceTokens mapIdentifier(Predicate<String> names, Consumer<PrintableToken> callback) {
        return this.mapIdentifier(names, callback, UnaryOperator.identity());
    }

    public SequenceTokens mapIdentifier(Predicate<String> names, Consumer<PrintableToken> callback, UnaryOperator<TokenTaskBuilder> parameters) {
        if (this.ignoredTokens.contains(TokenType.IDENTIFIER)) {
            throw new IllegalStateException("Cannot attempt to read an ignored token type: " + this.ignoredTokens);
        }

        this.expectedTokens.offer(newTask(new CallableAction(token -> token.type() == TokenType.IDENTIFIER && names.test(((CharSequenceToken) token).value()), callback, null), parameters));
        return this;
    }

    public SequenceTokens mapQualifiedName(Consumer<CharSequenceToken> nameCallback, Consumer<PrintableToken> dotCallback, @Nullable Consumer<SequenceTokens> partialAction) {
        return this.mapQualifiedName(nameCallback, dotCallback, type -> false, partialAction);
    }

    public SequenceTokens mapQualifiedName(Consumer<CharSequenceToken> nameCallback, Consumer<PrintableToken> dotCallback, Predicate<TokenType> transparentTokens, @Nullable Consumer<SequenceTokens> partialAction) {
        if (this.ignoredTokens.contains(TokenType.IDENTIFIER) || this.ignoredTokens.contains(TokenType.DOT)) {
            throw new IllegalStateException("Cannot attempt to read an ignored token type: " + this.ignoredTokens);
        }
        if (transparentTokens.test(TokenType.IDENTIFIER) || transparentTokens.test(TokenType.DOT)) {
            throw new IllegalArgumentException("Transparent tokens cannot be an identifier or a dot: " + transparentTokens);
        }

        this.expectedTokens.offer(newTask(new CallableQualifiedNameAction(nameCallback, dotCallback, transparentTokens, partialAction)));
        return this;
    }

    public SequenceTokens group(Consumer<SequenceTokens> subAction, UnaryOperator<TokenTaskBuilder> parameters) {
        this.expectedTokens.offer(newTask(new SubAction(subAction), parameters));
        return this;
    }

    public SequenceTokens either(Consumer<SequenceTokens> subAction) {
        this.expectedTokens.offer(newTask(new EitherAction(subAction), TokenTaskBuilder::asOptional));
        return this;
    }

    public SequenceTokens skip(TokenType type) {
        return this.skip(type, UnaryOperator.identity());
    }

    public SequenceTokens skip(TokenType type, UnaryOperator<TokenTaskBuilder> parameters) {
        return this.skip(type, null, parameters);
    }

    public SequenceTokens skip(TokenType type, @Nullable Consumer<SequenceTokens> subAction) {
        return this.skip(type, subAction, UnaryOperator.identity());
    }

    public SequenceTokens skip(TokenType type, @Nullable Consumer<SequenceTokens> subAction, UnaryOperator<TokenTaskBuilder> parameters) {
        if (this.ignoredTokens.contains(type)) {
            throw new IllegalStateException("Cannot attempt to skip an already ignored token type: " + type.name());
        }

        this.expectedTokens.offer(newTask(new SkipAction(token -> token.type() == type, subAction), parameters));
        return this;
    }

    public SequenceTokens skipIdentifier(Predicate<String> names) {
        return this.skipIdentifier(names, UnaryOperator.identity());
    }

    public SequenceTokens skipIdentifier(Predicate<String> names, UnaryOperator<TokenTaskBuilder> parameters) {
        return this.skipIdentifier(names, null, parameters);
    }

    public SequenceTokens skipIdentifier(Predicate<String> names, @Nullable Consumer<SequenceTokens> subAction) {
        return this.skipIdentifier(names, subAction, UnaryOperator.identity());
    }

    public SequenceTokens skipIdentifier(Predicate<String> names, @Nullable Consumer<SequenceTokens> subAction, UnaryOperator<TokenTaskBuilder> parameters) {
        if (this.ignoredTokens.contains(TokenType.IDENTIFIER)) {
            throw new IllegalStateException("Cannot attempt to skip an already ignored token type: " + this.ignoredTokens);
        }

        this.expectedTokens.offer(newTask(new SkipAction(token -> token.type() == TokenType.IDENTIFIER && names.test(((CharSequenceToken) token).value()), subAction), parameters));
        return this;
    }

    public SequenceTokens skipQualifiedName() {
        return this.skipQualifiedName(type -> false);
    }

    public SequenceTokens skipQualifiedName(Predicate<TokenType> transparentTokens) {
        return this.skipQualifiedName(transparentTokens, null);
    }

    public SequenceTokens skipQualifiedName(@Nullable Consumer<SequenceTokens> partialAction) {
        return this.skipQualifiedName(type -> false, partialAction);
    }

    public SequenceTokens skipQualifiedName(Predicate<TokenType> transparentTokens, @Nullable Consumer<SequenceTokens> partialAction) {
        if (this.ignoredTokens.contains(TokenType.IDENTIFIER) || this.ignoredTokens.contains(TokenType.DOT)) {
            throw new IllegalStateException("Cannot attempt to skip an already ignored token type: " + this.ignoredTokens);
        }
        if (transparentTokens.test(TokenType.IDENTIFIER) || transparentTokens.test(TokenType.DOT)) {
            throw new IllegalArgumentException("Transparent tokens cannot be an identifier or a dot: " + transparentTokens);
        }

        this.expectedTokens.offer(newTask(new CallableQualifiedNameAction(name -> {}, dot -> {}, transparentTokens, partialAction)));
        return this;
    }

    public SequenceTokens skipClosure(TokenType open, TokenType close, boolean nested) {
        return this.skipClosure(open, close, nested, UnaryOperator.identity());
    }

    public SequenceTokens skipClosure(TokenType open, TokenType close, boolean nested, UnaryOperator<TokenTaskBuilder> parameters) {
        this.expectedTokens.offer(newTask(new SkipClosureAction(open, close, nested), parameters));
        return this;
    }

    public boolean execute() {
        return this.execute(null);
    }

    public boolean executeOrThrow(BiFunction<TokenTaskThrowable, PrintableToken, Exception> failure) {
        return this.execute(failedTask -> {
            throw new RuntimeException(failure.apply(failedTask, (PrintableToken) this.iterator.peekPrevious()));
        });
    }

    public boolean executeOrThrow() {
        return this.execute(failedTask -> {
            throw new RuntimeException(failedTask.createFailure("Unexpected token found or a task failed to execute", (PrintableToken) this.iterator.peekPrevious()));
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
                if (task.isRepeatable()) {
                    alreadyRan = task.repeatCall();
                    if (!alreadyRan) {
                        task.runHook(HookType.FIRST, hook -> hook.pre().call(token));
                    }
                }

                task.runHook(HookType.EVERY, hook -> hook.pre().call(token));
                boolean success = task.run(token, this);
                task.runHook(HookType.EVERY, hook -> hook.post().call(token));

                if (!task.isRepeatable() || (success ? (alreadyRan && !this.iterator.hasNext()) : (alreadyRan || task.isOptional()))) {
                    if (task.isRepeatable()) {
                        task.runHook(HookType.LAST, hook -> hook.post().call(token));
                    }
                    this.expectedTokens.poll();
                    //this.expectedTokens.remove(task);
                }

                if (!success) {
                    if (task.isOptional() || alreadyRan) { // at least one run for repeat
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

    private boolean executeSub(Consumer<SequenceTokens> action) {
        SequenceTokens sequence = new SequenceTokens(this);
        action.accept(sequence);
        return sequence.execute((failedTask -> this.failedTask = failedTask));
    }

    private record EitherAction(Consumer<SequenceTokens> subAction) implements TokenAction {

        @Override
        public boolean execute(Token token, SequenceTokens executor) {
            executor.iterator().previous(); // get the initial token into the next pipe
            executor.executeSub(this.subAction);
            return false;
        }
    }

    private record SubAction(Consumer<SequenceTokens> subAction) implements TokenAction {

        @Override
        public boolean execute(Token token, SequenceTokens executor) {
            executor.iterator().previous(); // get the initial token into the next pipe
            return executor.executeSub(this.subAction);
        }
    }

    private record SkipAction(Predicate<Token> tokenPredicate, @Nullable Consumer<SequenceTokens> subAction) implements TokenAction {

        @Override
        public boolean execute(Token token, SequenceTokens executor) {
            boolean foundToken = tokenPredicate.test(token);
            if (foundToken && this.subAction != null) {
                return executor.executeSub(this.subAction);
            }
            return foundToken;
        }
    }

    private record SkipClosureAction(TokenType open, TokenType close, boolean nested) implements TokenAction {

        @Override
        public boolean execute(Token token, SequenceTokens executor) {
            if (token.type() != this.open) {
                return false;
            }

            NavigableToken iterator = executor.iterator();
            int depth = 1;
            while (iterator.hasNext()) {
                Token currentToken = iterator.next();
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

    private record CallableAction(Predicate<Token> tokenPredicate, Consumer<PrintableToken> callback, @Nullable Consumer<SequenceTokens> subAction) implements TokenAction {

        @Override
        public boolean execute(Token token, SequenceTokens executor) {
            if (this.tokenPredicate.test(token)) {
                this.callback.accept((PrintableToken) token);
                if (this.subAction != null) {
                    return executor.executeSub(this.subAction);
                }
                return true;
            }
            return false;
        }
    }

    private record CallableQualifiedNameAction(Consumer<CharSequenceToken> nameCallback, Consumer<PrintableToken> dotCallback, Predicate<TokenType> transparentTokens, @Nullable Consumer<SequenceTokens> partialAction) implements TokenAction {

        @Override
        public boolean execute(Token token, SequenceTokens executor) {
            if (token.type() != TokenType.IDENTIFIER) {
                return false;
            }

            this.nameCallback.accept((CharSequenceToken) token);

            NavigableToken iterator = executor.iterator();
            boolean expectDot = true;
            Integer expectNToken = null;
            Token lastToken = token;
            while (iterator.hasNext()) {
                PrintableToken currentToken = (PrintableToken) iterator.next();
                if (executor.ignoredTokens.contains(currentToken.type()) || this.transparentTokens.test(currentToken.type())) {
                    continue;
                }

                if (currentToken.type() != (expectDot ? TokenType.DOT : TokenType.IDENTIFIER)) {
                    iterator.previous();
                    if (expectNToken == null && !expectDot && this.partialAction != null && currentToken.type() == TokenType.AT_SIGN) { // annotation inside qn
                        if (!executor.executeSub(this.partialAction)) {
                            return false;
                        } else {
                            expectNToken = 1;
                            continue;
                        }
                    }
                    break;
                }
                lastToken = currentToken;

                if (!expectDot && SourceVersion.isKeyword(((CharSequenceToken) currentToken).value())) { // invalid name
                    return false;
                }

                if (expectNToken != null) {
                    if (expectNToken > 0) {
                        expectNToken--;
                    } else {
                        return false;
                    }
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

            if (this.partialAction != null && lastToken.type() == TokenType.DOT) { // .*
                return executor.executeSub(this.partialAction);
            }
            return false;
        }
    }
}
