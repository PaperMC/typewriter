package io.papermc.typewriter.preset;

import io.papermc.typewriter.preset.model.SwitchContent;
import io.papermc.typewriter.replace.SearchMetadata;
import io.papermc.typewriter.replace.SearchReplaceRewriter;

public abstract class SwitchRewriter extends SearchReplaceRewriter {

    protected abstract SwitchContent getContent();

    @Override
    protected void insert(SearchMetadata metadata, StringBuilder builder) {
        this.getContent().emitCode(metadata.indent(), this.indentUnit(), builder);
    }
}
