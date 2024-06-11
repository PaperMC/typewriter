package io.papermc.typewriter.preset.model;

import io.papermc.typewriter.IndentUnit;

public interface CodeEmitter {

    void emitCode(String indent, IndentUnit indentUnit, StringBuilder builder);
}
