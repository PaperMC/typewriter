package io.papermc.typewriter.preset;

import io.papermc.typewriter.preset.model.SwitchBody;
import io.papermc.typewriter.replace.SearchMetadata;
import io.papermc.typewriter.replace.SearchReplaceRewriter;

public abstract class SwitchRewriter extends SearchReplaceRewriter {

    protected abstract SwitchBody getBody();

    @Override
    protected void insert(SearchMetadata metadata, StringBuilder builder) {
        this.getBody().emitCode(metadata.indent(), this.indentUnit(), builder);
    }
}
