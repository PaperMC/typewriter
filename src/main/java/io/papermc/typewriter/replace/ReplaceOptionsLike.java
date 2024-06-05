package io.papermc.typewriter.replace;

import org.jetbrains.annotations.Contract;

public interface ReplaceOptionsLike {

    @Contract(pure = true)
    ReplaceOptions asOptions();
}
