package io.papermc.typewriter.replace;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.FileMetadata;
import io.papermc.typewriter.ImportLayout;
import io.papermc.typewriter.IndentUnit;
import io.papermc.typewriter.SourceFile;
import io.papermc.typewriter.SourceRewriter;
import io.papermc.typewriter.context.ImportCollector;
import io.papermc.typewriter.context.ImportTypeCollector;
import io.papermc.typewriter.parser.ImportParser;
import io.papermc.typewriter.parser.Lexer;
import io.papermc.typewriter.parser.StringReader;
import io.papermc.typewriter.parser.exception.ReaderException;
import io.papermc.typewriter.parser.token.TokenBlockPosition;
import io.papermc.typewriter.util.ClassNamedView;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.CharArrayReader;
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
    public void writeToFile(Path parent, Path writeFolder, FileMetadata fileMetadata, SourceFile file) throws IOException {
        Path filePath = file.path();

        Path path = parent.resolve(filePath);
        StringBuilder content = new StringBuilder();

        if (Files.isRegularFile(path)) {
            final Lexer lex;
            final ImportTypeCollector collector;
            try (BufferedReader buffer = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                lex = Lexer.fromReader(buffer);
                collector = collectImport(file, lex);
            } catch (ReaderException ex) {
                throw ex.withAdditionalContext(file);
            }

            this.setup(file, fileMetadata, new ClassNamedView(parent, 20, null), collector);

            try (BufferedReader reader = new BufferedReader(new CharArrayReader(lex.toCharArray()))) {
                searchAndReplace(file, fileMetadata, reader, content);
            }

            if (collector.isModified()) { // if added entries
                // rewrite the imports
                this.rewriteImports(collector, fileMetadata.importLayout().getRelevantSection(path), content);
            }
        } else {
            LOGGER.warn("Target source file '{}' doesn't exists, dumping rewriters data instead...", filePath);
            this.setup(file, fileMetadata, new ClassNamedView(parent, 20, null), ImportCollector.NO_OP);
            dumpAll(file, fileMetadata, content);
            filePath = filePath.resolveSibling(filePath.getFileName() + ".dump");
            path = parent.resolve(filePath);
        }

        if (!writeFolder.equals(parent)) { // todo remove
            path = writeFolder.resolve(filePath);
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private void setup(SourceFile source, FileMetadata fileMetadata, ClassNamedView classNamedView, @Nullable ImportCollector importCollector) {
        this.getRewriters().forEach(rewriter -> {
            rewriter.source = source;
            rewriter.fileMetadata = fileMetadata;
            rewriter.classNamedView = classNamedView;
            rewriter.importCollector = importCollector;
        });
    }

    private void dumpAll(SourceFile file, FileMetadata fileMetadata, StringBuilder content) {
        IndentUnit indentUnit = file.indentUnit().orElse(fileMetadata.indentUnit());

        content.append("Dump of the rewriters that apply to the file : ").append(file.path());
        content.append('\n');
        content.append('\n');

        content.append("Configuration :");
        content.append('\n');
        content.append("Indent unit : \"").append(indentUnit.content()).append("\" (").append(indentUnit.size()).append(" char)");
        content.append('\n');
        content.append("Indent char : '").append(indentUnit.character()).append("' (U+%04X)".formatted((int) indentUnit.character()));
        content.append('\n');

        for (SearchReplaceRewriter rewriter : this.getRewriters()) {
            content.append('\n');
            rewriter.dump(content);
        }
    }

    private ImportTypeCollector collectImport(SourceFile source, Lexer lexer) {
        final ImportTypeCollector importCollector = new ImportTypeCollector(source.mainClass());
        ImportParser.collectImports(lexer, importCollector);
        return importCollector;
    }

    private void searchAndReplace(SourceFile file, FileMetadata fileMetadata, BufferedReader reader, StringBuilder content) throws IOException {
        Set<SearchReplaceRewriter> rewriters = this.getRewriters();
        Preconditions.checkState(!rewriters.isEmpty());

        Set<SearchReplaceRewriter> remainingRewriters = new HashSet<>(rewriters);
        Set<SearchReplaceRewriter> unusedRewriters = new HashSet<>(rewriters);
        @Nullable StringBuilder strippedContent = null;

        IndentUnit indentUnit = file.indentUnit().orElse(fileMetadata.indentUnit());
        @Nullable String indent = null;
        @Nullable SearchReplaceRewriter foundRewriter = null;

        int i = 0;
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }

            StringReader lineIterator = new StringReader(line);
            CommentMarker marker = EMPTY_MARKER;
            if (!line.isEmpty()) {
                marker = searchMarker(
                    lineIterator,
                    foundRewriter == null ? null : indent,
                    indentUnit,
                    remainingRewriters,
                    foundRewriter == null
                );
            }

            if (marker != EMPTY_MARKER) {
                if (foundRewriter != null) {
                    if (!marker.owner().equals(foundRewriter)) {
                        throw new IllegalStateException("Generated end comment doesn't match for rewriter " + foundRewriter.getName() + " in " + file.mainClass().canonicalName() + " at line " + (i + 1));
                    }

                    if (!foundRewriter.options.exactReplacement()) {
                        // append generated comment
                        if (foundRewriter.options.generatedComment().isPresent()) {
                            content.append(indent).append("// ").append(foundRewriter.options.generatedComment().get());
                            content.append('\n');
                        }

                        foundRewriter.insert(new SearchMetadata(indent, strippedContent.toString(), i), content);
                        strippedContent = null;
                    }
                    if (!foundRewriter.options.multipleOperation()) {
                        remainingRewriters.remove(foundRewriter);
                    }
                    unusedRewriters.remove(foundRewriter);
                    foundRewriter = null;
                } else {
                    if (marker.indentSize() % indentUnit.size() != 0) {
                        throw new IllegalStateException("Generated start comment is not properly indented at line " + (i + 1) + " for rewriter " + marker.owner().getName() + " in " + file.mainClass().canonicalName());
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
                    foundRewriter.replaceLine(new SearchMetadata(indent, line, i), content);
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
            throw new IllegalStateException("Generated end comment is missing for rewriter " + foundRewriter.getName() + " in " + file.mainClass().canonicalName());
        }

        if (!unusedRewriters.isEmpty()) {
            throw new IllegalStateException("SRT didn't found some expected generated comment for the following rewriters: " + unusedRewriters.stream().map(SearchReplaceRewriter::getName).toList());
        }
    }

    private void rewriteImports(ImportTypeCollector collector, ImportLayout.Section layout, StringBuilder into) {
        Lexer lex = new Lexer(into.toString().toCharArray());
        TokenBlockPosition position = ImportParser.trackImportPosition(lex); // need to retrack this just in case other rewriters moved things around
        into.replace(position.startPos.cursor(), position.endPos.cursor(), collector.writeImports(layout));
    }

    @VisibleForTesting
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
