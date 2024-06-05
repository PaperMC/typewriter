package io.papermc.typewriter.replace;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public record CommentMarker(SearchReplaceRewriter owner, int indent) {

    private CommentMarker() {
        this(null, 0);
    }

    public static final CommentMarker EMPTY_MARKER = new CommentMarker();
}
