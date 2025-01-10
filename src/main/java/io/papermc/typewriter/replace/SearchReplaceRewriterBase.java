package io.papermc.typewriter.replace;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.context.FileMetadata;
import io.papermc.typewriter.context.SourcesMetadata;
import io.papermc.typewriter.context.ImportLayout;
import io.papermc.typewriter.context.IndentUnit;
import io.papermc.typewriter.SourceFile;
import io.papermc.typewriter.SourceRewriter;
import io.papermc.typewriter.context.ImportCollector;
import io.papermc.typewriter.context.ImportNameCollector;
import io.papermc.typewriter.parser.ImportParser;
import io.papermc.typewriter.parser.Lexer;
import io.papermc.typewriter.parser.StringReader;
import io.papermc.typewriter.parser.exception.ReaderException;
import io.papermc.typewriter.parser.token.pos.TokenCapture;
import io.papermc.typewriter.util.ClassNamedView;
import io.papermc.typewriter.util.ClassResolver;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.LineNumberReader;
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
    public void writeToFile(Path parent, SourcesMetadata sourcesMetadata, ClassResolver resolver, ClassNamedView view, SourceFile file) throws IOException {
        Path filePath = file.path();

        final Path path = parent.resolve(filePath);
        Path destinationPath;
        StringBuilder content = new StringBuilder();

        if (Files.isRegularFile(path)) {
            final Lexer lex;
            try (BufferedReader buffer = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                lex = Lexer.fromReader(buffer);
                lex.checkMarkdownDocComments = !sourcesMetadata.canSkipMarkdownDocComments();
            } catch (ReaderException ex) {
                throw ex.withAdditionalContext(file);
            }

            ImportNameCollector collector = collectImport(file, lex);
            this.setup(file, sourcesMetadata, resolver, view, collector);

            try (LineNumberReader reader = new LineNumberReader(new CharArrayReader(lex.toCharArray()))) {
                searchAndReplace(file, sourcesMetadata, reader, content);
            }

            if (collector.isModified()) { // if added entries
                // rewrite the imports
                this.rewriteImports(collector, file.metadata().flatMap(FileMetadata::header).orElseGet(() -> sourcesMetadata.importLayout().getRelevantHeader(path, ImportLayout.Header.DEFAULT)), content);
            }
            destinationPath = path;
        } else {
            LOGGER.warn("Target source file '{}' doesn't exists, dumping rewriters data instead...", filePath);
            this.setup(file, sourcesMetadata, resolver, view, ImportCollector.NO_OP);
            dumpAll(file, sourcesMetadata, content);
            filePath = filePath.resolveSibling(filePath.getFileName() + ".dump");
            destinationPath = parent.resolve(filePath);
        }

        Files.writeString(destinationPath, content, StandardCharsets.UTF_8);
    }

    private void setup(SourceFile source, SourcesMetadata sourcesMetadata, ClassResolver classResolver, ClassNamedView classNamedView, @Nullable ImportCollector importCollector) {
        this.getRewriters().forEach(rewriter -> {
            rewriter.source = source;
            rewriter.sourcesMetadata = sourcesMetadata;
            rewriter.classResolver = classResolver;
            rewriter.classNamedView = classNamedView;
            rewriter.importCollector = importCollector;
        });
    }

    private void dumpAll(SourceFile file, SourcesMetadata metadata, StringBuilder content) {
        IndentUnit indentUnit = file.metadata().flatMap(FileMetadata::indentUnit).orElse(metadata.indentUnit());

        content.append("Dump of the rewriters that apply to the file: ").append(file.path());
        content.append('\n');
        content.append('\n');

        content.append("Configuration:");
        content.append('\n');
        content.append("Indent unit: \"").append(indentUnit.content()).append("\" (").append(indentUnit.size()).append(" char)");
        content.append('\n');
        content.append("Indent char: '").append(indentUnit.character()).append("' (U+%04X)".formatted((int) indentUnit.character()));
        content.append('\n');

        for (SearchReplaceRewriter rewriter : this.getRewriters()) {
            content.append('\n');
            rewriter.dump(content);
        }
    }

    private ImportNameCollector collectImport(SourceFile source, Lexer lexer) {
        final ImportNameCollector importCollector = new ImportNameCollector(source.mainClass());
        ImportParser.collectImports(lexer, importCollector, source);
        return importCollector;
    }

    private void searchAndReplace(SourceFile file, SourcesMetadata metadata, LineNumberReader reader, StringBuilder content) throws IOException {
        Set<SearchReplaceRewriter> rewriters = this.getRewriters();
        Preconditions.checkState(!rewriters.isEmpty());

        Set<SearchReplaceRewriter> remainingRewriters = new HashSet<>(rewriters);
        Set<SearchReplaceRewriter> unusedRewriters = new HashSet<>(rewriters);
        @Nullable StringBuilder strippedContent = null;

        IndentUnit indentUnit = file.metadata().flatMap(FileMetadata::indentUnit).orElse(metadata.indentUnit());
        @Nullable String indent = null;
        @Nullable SearchReplaceRewriter foundRewriter = null;

        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }

            CommentMarker marker = EMPTY_MARKER;
            if (!line.isEmpty()) {
                StringReader lineIterator = new StringReader(line);
                if (foundRewriter == null) {
                    marker = searchStartMarker(
                        lineIterator,
                        indentUnit,
                        remainingRewriters
                    );
                } else {
                    marker = searchEndMarker(
                        lineIterator,
                        indent,
                        foundRewriter
                    );
                }
            }

            if (marker != EMPTY_MARKER) {
                if (foundRewriter != null) {
                    if (!foundRewriter.options.exactReplacement()) {
                        // append generated comment
                        if (foundRewriter.options.generatedComment().isPresent()) {
                            content.append(indent).append("// ").append(foundRewriter.options.generatedComment().get());
                            content.append('\n');
                        }

                        foundRewriter.insert(new SearchMetadata(indent, strippedContent.toString(), reader.getLineNumber() - 1), content);
                        strippedContent = null;
                    }
                    if (!foundRewriter.options.multipleOperation()) {
                        remainingRewriters.remove(foundRewriter);
                    }
                    unusedRewriters.remove(foundRewriter);
                    foundRewriter = null;
                } else {
                    if (marker.indentSize() % indentUnit.size() != 0) {
                        throw new IllegalStateException("Generated start comment is not properly indented at line %d for rewriter %s in %s".formatted(reader.getLineNumber(), marker.owner().getName(), file.mainClass().canonicalName()));
                    }
                    indent = String.valueOf(indentUnit.character()).repeat(marker.indentSize()); // update indent based on the comments for flexibility

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
                    foundRewriter.replaceLine(new SearchMetadata(indent, line, reader.getLineNumber() - 1), content);
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
        }

        if (foundRewriter != null) {
            throw new IllegalStateException("Generated end comment is missing for rewriter " + foundRewriter.getName() + " in " + file.mainClass().canonicalName());
        }

        if (!unusedRewriters.isEmpty()) {
            throw new IllegalStateException("SRT didn't found some expected generated comment for the following rewriters: " + unusedRewriters.stream().map(SearchReplaceRewriter::getName).toList());
        }
    }

    private void rewriteImports(ImportNameCollector collector, ImportLayout.Header header, StringBuilder into) {
        Lexer lex = new Lexer(into.toString().toCharArray());
        TokenCapture position = ImportParser.trackImportPosition(lex); // need to retrack this just in case other rewriters moved things around
        into.replace(position.start().cursor(), position.end().cursor(), collector.writeImports(header));
    }

    @VisibleForTesting
    public CommentMarker searchStartMarker(StringReader lineIterator, IndentUnit indentUnit, Set<SearchReplaceRewriter> remainingRewriters) {
        int indentSize = lineIterator.skipChars(indentUnit.character());
        if (!lineIterator.trySkipString("// ")) {
            return EMPTY_MARKER;
        }

        @Nullable SearchReplaceRewriter result = null;
        for (SearchReplaceRewriter rewriter : remainingRewriters) {
            boolean found = lineIterator.trySkipString(rewriter.options.startCommentMarker());
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

    private CommentMarker searchEndMarker(StringReader lineIterator, String indent, SearchReplaceRewriter currentRewriter) {
        if (!indent.isEmpty() && !lineIterator.trySkipChars(indent.length(), indent.charAt(0))) {
            return EMPTY_MARKER;
        }
        if (!lineIterator.trySkipString("// ")) {
            return EMPTY_MARKER;
        }
        if (!lineIterator.trySkipString(currentRewriter.options.endCommentMarker()) || lineIterator.canRead()) {
            return EMPTY_MARKER;
        }

        return new CommentMarker(currentRewriter, indent.length());
    }

    public abstract Set<SearchReplaceRewriter> getRewriters();
}
