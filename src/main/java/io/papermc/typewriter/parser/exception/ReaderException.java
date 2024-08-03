package io.papermc.typewriter.parser.exception;

import io.papermc.typewriter.SourceFile;
import org.jetbrains.annotations.Contract;

public class ReaderException extends IllegalStateException {

    protected SourceFile file;

    protected ReaderException(String message) {
        super(message);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public ReaderException withAdditionalContext(SourceFile file) {
        this.file = file;
        return this;
    }
}
