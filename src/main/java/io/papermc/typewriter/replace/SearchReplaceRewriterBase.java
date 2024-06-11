package io.papermc.typewriter.replace;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.IndentUnit;
import io.papermc.typewriter.SourceFile;
import io.papermc.typewriter.SourceRewriter;
import io.papermc.typewriter.context.ImportCollector;
import io.papermc.typewriter.context.ImportTypeCollector;
import io.papermc.typewriter.parser.LineParser;
import io.papermc.typewriter.parser.ParserException;
import io.papermc.typewriter.parser.StringReader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static io.papermc.typewriter.replace.CommentMarker.EMPTY_MARKER;

@DefaultQualifier(NonNull.class)
public abstract class SearchReplaceRewriterBase implements SourceRewriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchReplaceRewriterBase.class);

    @Override
    public void writeToFile(Path parent, Path writeFolder, SourceFile file) throws IOException {
        Path filePath = file.path();

        Path path = parent.resolve(filePath);
        StringBuilder content = new StringBuilder();

        if (Files.isRegularFile(path)) {
            try (BufferedReader buffer = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                searchAndReplace(file, buffer, content);
            }
        } else {
            LOGGER.warn("Target source file '{}' doesn't exists, dumping rewriters data instead...", filePath);
            dumpAll(file, content);
            filePath = filePath.resolveSibling(filePath.getFileName() + ".dump");
            path = parent.resolve(filePath);
        }

        if (!writeFolder.equals(parent)) { // todo remove
            path = writeFolder.resolve(filePath);
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private void dumpAll(SourceFile file, StringBuilder content) {
        content.append("Dump of the rewriters that apply to the file : ").append(file.path());
        content.append('\n');
        content.append('\n');

        content.append("Configuration :");
        content.append('\n');
        content.append("Indent unit : \"").append(file.indentUnit().content()).append("\" (").append(file.indentUnit().size()).append(" char)");
        content.append('\n');
        content.append("Indent char : '").append(file.indentUnit().character()).append("' (").append((int) file.indentUnit().character()).append(")");
        content.append('\n');

        for (SearchReplaceRewriter rewriter : this.getRewriters()) {
            content.append('\n');
            rewriter.dump(file, content);
        }
    }

    private void searchAndReplace(SourceFile source, BufferedReader reader, StringBuilder content) throws IOException {
        Set<SearchReplaceRewriter> rewriters = this.getRewriters();
        Preconditions.checkState(!rewriters.isEmpty());

        Set<SearchReplaceRewriter> remainingRewriters = new HashSet<>(rewriters);
        Set<SearchReplaceRewriter> unusedRewriters = new HashSet<>(rewriters);
        @Nullable StringBuilder strippedContent = null;

        final ImportCollector importCollector = new ImportTypeCollector(source.mainClass());
        final LineParser lineParser = new LineParser();

        @Nullable String indent = null;
        @Nullable SearchReplaceRewriter foundRewriter = null;
        boolean inBody = false;
        int i = 0;
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }

            StringReader lineIterator = new StringReader(line);
            // collect import to avoid fqn when not needed
            int previousCursor = lineIterator.getCursor();
            if (importCollector != ImportCollector.NO_OP && !inBody && foundRewriter == null && !line.isEmpty()) {
                final boolean reachBody;
                try {
                    reachBody = lineParser.consumeImports(lineIterator, importCollector);
                } catch (ParserException ex) {
                    throw ex.withAdditionalContext(source, i + 1);
                }
                if (reachBody) {
                    inBody = true;
                }
            }
            lineIterator.setCursor(previousCursor);

            CommentMarker marker = EMPTY_MARKER;
            if (!line.isEmpty()) {
                marker = searchMarker(
                    lineIterator,
                    foundRewriter == null ? null : indent,
                    source.indentUnit(),
                    remainingRewriters,
                    foundRewriter == null
                );
            }

            if (marker != EMPTY_MARKER) {
                if (foundRewriter != null) {
                    if (!marker.owner().equals(foundRewriter)) {
                        throw new IllegalStateException("Generated end comment doesn't match for rewriter " + foundRewriter.getName() + " in " + source.mainClass().canonicalName() + " at line " + (i + 1));
                    }

                    if (!foundRewriter.options.exactReplacement()) {
                        // append generated comment
                        if (foundRewriter.options.generatedComment().isPresent()) {
                            content.append(indent).append("// ").append(foundRewriter.options.generatedComment().get());
                            content.append('\n');
                        }

                        foundRewriter.insert(new SearchMetadata(source, importCollector, indent, strippedContent.toString(), i), content);
                        strippedContent = null;
                    }
                    if (!foundRewriter.options.multipleOperation()) {
                        remainingRewriters.remove(foundRewriter);
                    }
                    unusedRewriters.remove(foundRewriter);
                    foundRewriter = null;
                } else {
                    if (marker.indentSize() % source.indentUnit().size() != 0) {
                        throw new IllegalStateException("Generated start comment is not properly indented at line " + (i + 1) + " for rewriter " + marker.owner().getName() + " in " + source.mainClass().canonicalName());
                    }
                    indent = " ".repeat(marker.indentSize()); // update indent based on the comments for flexibility

                    foundRewriter = marker.owner();
                    if (!foundRewriter.options.exactReplacement()) {
                        strippedContent = new StringBuilder();
                    }
                }
            }

            @Nullable StringBuilder usedBuilder = null;
            if (marker == EMPTY_MARKER && foundRewriter != null) {
                if (foundRewriter.options.exactReplacement()) {
                    // there's no generated comment here since when the size is equals the replaced content doesn't depend on the game content
                    // if it does that means the replaced content might not be equals during MC update because of adding/removed content
                    foundRewriter.replaceLine(new SearchMetadata(source, importCollector, indent, line, i), content);
                } else {
                    usedBuilder = strippedContent;
                }
            } else {
                usedBuilder = content;
            }
            if (usedBuilder != null) {
                usedBuilder.append(line);
                usedBuilder.append('\n');
            }
            i++;
        }

        if (foundRewriter != null) {
            throw new IllegalStateException("Generated end comment is missing for rewriter " + foundRewriter.getName() + " in " + source.mainClass().canonicalName());
        }

        if (!unusedRewriters.isEmpty()) {
            throw new IllegalStateException("SRT didn't found some expected generated comment for the following rewriters: " + unusedRewriters.stream().map(SearchReplaceRewriter::getName).toList());
        }
    }

    public CommentMarker searchMarker(StringReader lineIterator, @Nullable String indent, IndentUnit indentUnit, Set<SearchReplaceRewriter> remainingRewriters, boolean searchStart) {
        boolean strict = indent != null;
        final int indentSize;
        if (strict) {
            if (!indent.isEmpty() && !lineIterator.trySkipChars(indent.length(), indent.charAt(0))) {
                return EMPTY_MARKER;
            }
            indentSize = indent.length();
        } else {
            indentSize = lineIterator.skipChars(indentUnit.character());
        }

        if (!lineIterator.trySkipString("// ")) {
            return EMPTY_MARKER;
        }

        @Nullable SearchReplaceRewriter result = null;
        for (SearchReplaceRewriter rewriter : remainingRewriters) {
            boolean found = lineIterator.trySkipString(searchStart ? rewriter.options.startCommentMarker() : rewriter.options.endCommentMarker());
            if (found) {
                result = rewriter;
                break;
            }
        }

        if (lineIterator.canRead() || result == null) {
            return EMPTY_MARKER;
        }

        return new CommentMarker(result, indentSize);
    }

    public abstract Set<SearchReplaceRewriter> getRewriters();
}
