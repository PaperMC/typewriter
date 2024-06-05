package io.papermc.typewriter.parser;

import io.papermc.typewriter.parser.closure.ClosureType;

public enum ClosureAdvanceResult {
    /**
     * When the pointer advance on a closure. Escaped closure are skipped!
     */
    CHANGED,
    /**
     * When the pointer doesn't advance and cannot find the closure.
     */
    IGNORED,
    /**
     * When the pointer advance by reaching a leaf closure escaped by '\' char preceding it.
     *
     * @see ClosureType#escapableByPreviousChar()
     */
    SKIPPED
}
