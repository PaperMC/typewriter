package io.papermc.typewriter.replace;

/**
 * Search metadata bound to a rewriter with the data of the current replacement.
 *
 * @param indent          the indent adjusted by the start comment marker
 * @param replacedContent the replaced content
 * @param line            the line number approximately starting from 0 or -1 during a dump
 */
public record SearchMetadata(String indent, String replacedContent, int line) {
}
