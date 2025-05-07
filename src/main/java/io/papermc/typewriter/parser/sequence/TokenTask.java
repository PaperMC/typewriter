package io.papermc.typewriter.parser.sequence;

import io.papermc.typewriter.parser.exception.ParserException;
import io.papermc.typewriter.parser.sequence.hook.Callback;
import io.papermc.typewriter.parser.sequence.hook.HookManager;
import io.papermc.typewriter.parser.sequence.hook.HookType;
import io.papermc.typewriter.parser.sequence.hook.Hooks;
import io.papermc.typewriter.parser.token.PrintableToken;
import io.papermc.typewriter.parser.token.Token;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.function.Consumer;

public class TokenTask implements TokenTaskThrowable {

    private final TokenAction action;
    private final boolean optional;
    private final boolean repeatable;
    private final @MonotonicNonNull HookManager hookManager;
    PrintableToken lastInput;

    public TokenTask(TokenAction action, boolean optional, boolean repeatable, HookManager hookManager) {
        this.action = action;
        this.optional = optional;
        this.repeatable = repeatable;
        this.hookManager = hookManager;
    }

    public boolean isOptional() {
        return this.optional;
    }

    public boolean isRepeatable() {
        return this.repeatable;
    }

    boolean alreadyRan() {
        return this.lastInput != null;
    }

    boolean run(Token token, SequenceTokens executor) {
        boolean alreadyRan = alreadyRan();
        if (isRepeatable() && !alreadyRan) {
            runHook(HookType.FIRST, hook -> hook.pre().call(token));
        }

        runHook(HookType.EVERY, hook -> hook.pre().call(token));
        boolean success = this.action.execute(token, executor);
        runHook(HookType.EVERY, hook -> hook.post().call(token));

        if (isRepeatable() && (success ? (alreadyRan && !executor.iterator().hasNext()) : (alreadyRan || isOptional()))) { // based in SequenceTokens#execute poll
            runHook(HookType.LAST, hook -> hook.post().call(token));
        }

        this.lastInput = (PrintableToken) token;
        return success;
    }

    private void runHook(HookType type, Consumer<Callback> callback) {
        if (this.hookManager == null) {
            return;
        }

        this.hookManager.fire(type, callback);
    }

    static class Builder implements TokenTaskBuilder {
        private final TokenAction action;
        private boolean optional;
        private boolean repeatable;
        private @MonotonicNonNull HookManager hookManager;

        Builder(TokenAction action) {
            this.action = action;
        }

        @Override
        public Builder asOptional() {
            this.optional = true;
            return this;
        }

        @Override
        public Builder asRepeatable() {
            this.repeatable = true;
            return this;
        }

        @Override
        public Builder hooks(Consumer<Hooks> manager) {
            if (this.hookManager == null) {
                this.hookManager = new HookManager();
            } else {
                throw new IllegalStateException("Cannot configure hooks twice!");
            }

            manager.accept(this.hookManager);
            this.hookManager.finishCallback();
            return this;
        }

        public TokenTask build() {
            return new TokenTask(
                this.action,
                this.optional,
                this.repeatable,
                this.hookManager
            );
        }
    }

    @Override
    public FailureException createFailure(String message, PrintableToken token) {
        return new FailureException(message, token);
    }

    public class FailureException extends ParserException {

        public FailureException(String message, PrintableToken token) {
            super(message, token);
        }

        @Override
        public String getMessage() {
            return "%s during execution of task: %s".formatted(super.getMessage(), TokenTask.this.action);
        }
    }
}
