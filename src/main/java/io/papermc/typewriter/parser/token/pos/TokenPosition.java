package io.papermc.typewriter.parser.token.pos;

class TokenPosition<T> extends AbstractTokenPosition<T> implements TokenRecorder.Default<T> {

    TokenPosition(TokenSnapshot.Default<T> snapshotter) {
        super(snapshotter);
    }

    @Override
    public void begin(T object) {
        super.tryTakeStart(object);
    }

    @Override
    public void end(T object) {
        super.tryTakeEnd(object);
    }

    static class Constant<T> extends AbstractTokenPosition<T> implements TokenRecorder.Constant {

        private final T constant;

        Constant(TokenSnapshot.Constant<T> snapshotter, T constant) {
            super(snapshotter);
            this.constant = constant;
        }

        @Override
        public void begin() {
            super.tryTakeStart(this.constant);
        }

        @Override
        public void end() {
            super.tryTakeEnd(this.constant);
        }
    }
}
