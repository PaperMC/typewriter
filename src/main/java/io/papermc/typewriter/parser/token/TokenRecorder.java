package io.papermc.typewriter.parser.token;

public interface TokenRecorder {

    record AbsolutePos(int cursor, int row, int column) {}
}
