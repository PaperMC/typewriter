package io.papermc.typewriter.parser.token;

import java.util.function.IntSupplier;

public final class TokenPosition implements TokenRecorder {

    private final IntSupplier cursorGetter;
    private final IntSupplier rowGetter;
    private final IntSupplier columnGetter;

    private boolean recordEnd;

    public AbsolutePos startPos;
    public AbsolutePos endPos;

    private TokenPosition(IntSupplier cursorGetter, IntSupplier rowGetter, IntSupplier columnGetter) {
        this.cursorGetter = cursorGetter;
        this.rowGetter = rowGetter;
        this.columnGetter = columnGetter;
    }

    public static TokenPosition record(IntSupplier cursorGetter, IntSupplier rowGetter, IntSupplier columnGetter) {
        return new TokenPosition(cursorGetter, rowGetter, columnGetter);
    }

    public void begin() {
        this.begin(false);
    }

    public void begin(boolean recordEnd) {
        if (this.startPos != null) {
            throw new UnsupportedOperationException("Cannot begin a token position twice");
        }
        this.recordEnd = recordEnd;
        this.startPos = this.getCapture();
    }

    public void end() {
        if (this.recordEnd) {
            this.endPos = this.getCapture();
        }
    }

    private AbsolutePos getCapture() {
        return new AbsolutePos(
            this.cursorGetter.getAsInt(),
            this.rowGetter.getAsInt(),
            this.columnGetter.getAsInt()
        );
    }
}
