package io.papermc.typewriter.util;

import java.util.function.Consumer;

public interface WeakenConsumer<E extends T, T> extends Consumer<E> {

    default void call(T token) {
        this.accept((E) token);
    }
}
