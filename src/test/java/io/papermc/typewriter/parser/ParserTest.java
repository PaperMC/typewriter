package io.papermc.typewriter.parser;

import io.papermc.typewriter.context.ImportCollector;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Tag;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

@Tag("parser")
public class ParserTest {

    protected static final Path CONTAINER = Path.of(System.getProperty("user.dir"), "src/testData/java");

    protected void parseFile(Path path, ImportCollector importCollector) throws IOException {
        parseFile(path, importCollector, null, null);
    }

    protected void parseFile(Path path, ImportCollector importCollector, @Nullable Consumer<StringReader> enterBodyCallback, @Nullable Runnable eofCallback) throws IOException {
        final LineParser lineParser = new LineParser();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            while (true) {
                String textLine = reader.readLine();
                if (textLine == null) {
                    if (eofCallback != null) {
                        eofCallback.run();
                    }
                    break;
                }

                if (!textLine.isEmpty()) {
                    StringReader line = new StringReader(textLine);
                    if (lineParser.consumeImports(line, importCollector)) {
                        if (enterBodyCallback != null) {
                            enterBodyCallback.accept(line);
                        }
                        break;
                    }
                }
            }
        }
    }
}
