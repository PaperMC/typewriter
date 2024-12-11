package io.papermc.typewriter.parser.sequence;

public interface TokenTaskThrowable {

    TokenTask.FailureException createFailure(String message);
}
