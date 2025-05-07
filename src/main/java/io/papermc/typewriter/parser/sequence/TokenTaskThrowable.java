package io.papermc.typewriter.parser.sequence;

import io.papermc.typewriter.parser.token.PrintableToken;

public interface TokenTaskThrowable {

    TokenTask.FailureException createFailure(String message, PrintableToken token);
}
