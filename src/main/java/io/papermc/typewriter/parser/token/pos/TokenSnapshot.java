package io.papermc.typewriter.parser.token.pos;

public sealed abstract class TokenSnapshot<T> permits TokenSnapshot.Default, TokenSnapshot.Constant {

    abstract AbsolutePos takeStart(T object);

    abstract AbsolutePos takeEnd(T object);

    public static abstract non-sealed class Default<T> extends TokenSnapshot<T> {

        public TokenRecorder.Default<T> record() {
            return new TokenPosition<>(this);
        }
    }

    public static abstract non-sealed class Constant<T> extends TokenSnapshot<T> {

        public abstract AbsolutePos take(T object);

        @Override
        AbsolutePos takeStart(T object) {
            return this.take(object);
        }

        @Override
        AbsolutePos takeEnd(T object) {
            return this.take(object);
        }

        public TokenRecorder.Constant record(T from) {
            return new TokenPosition.Constant<>(this, from);
        }
    }
}
