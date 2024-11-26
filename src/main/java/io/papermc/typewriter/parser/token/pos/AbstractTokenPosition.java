package io.papermc.typewriter.parser.token.pos;

class AbstractTokenPosition<T> implements TokenRecorder {

    private final TokenSnapshot<T> snapshotter;
    private AbsolutePos startPos;
    private AbsolutePos endPos;

    protected AbstractTokenPosition(TokenSnapshot<T> snapshotter) {
        this.snapshotter = snapshotter;
    }

    protected void tryTakeStart(T object) {
        if (this.startPos != null) {
            throw new UnsupportedOperationException("Cannot begin a token position twice");
        }

        this.ensureNotFrozen();
        this.startPos = this.snapshotter.takeStart(object);
    }

    protected void tryTakeEnd(T object) {
        if (this.startPos == null) {
            throw new UnsupportedOperationException("Cannot end a token position before starting it");
        }

        this.ensureNotFrozen();
        this.endPos = this.snapshotter.takeEnd(object);
    }

    @Override
    public boolean isInProgress() {
        return this.startPos != null && this.endPos == null;
    }

    private TokenCapture capture;

    @Override
    public TokenCapture fetch() {
        if (this.capture == null) {
            if (this.isInProgress()) {
                throw new UnsupportedOperationException("Snapshot is still not finished");
            }

            this.capture = new TokenCapture(this.startPos, this.endPos);
        }

        return this.capture;
    }

    private void ensureNotFrozen() {
        if (this.capture != null) {
            throw new UnsupportedOperationException("Cannot interact with this record anymore");
        }
    }
}
