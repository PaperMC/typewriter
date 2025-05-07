package io.papermc.typewriter.parser;

import org.jetbrains.annotations.ApiStatus;

public enum JavaFeature {
    MARKDOWN_DOC_COMMENTS(23),
    @ApiStatus.Experimental
    MODULE_IMPORT(25);

    public final int requiredVersion; // GA
    JavaFeature(int requiredVersion) {
        this.requiredVersion = requiredVersion;
    }
}
