package io.papermc.typewriter.utils;

import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.SourceFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.stream.Stream;

/**
 * A class named view search through a specified path a
 * java source file and convert it to {@link ClassNamed} if possible.
 * <br>
 * Do note that the class is not known at runtime though.
 * <br>
 * The max depth represents the number of sub folders
 * taken into account.
 */
public class ClassNamedView {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassNamedView.class);
    private final Path base;
    private final int maxDepth;

    public ClassNamedView(Path base, int maxDepth) {
        this.base = base;
        this.maxDepth = maxDepth;
    }

    public ClassNamed findFirst(String name) {
        int dotIndex = name.indexOf('.');
        if (dotIndex != -1) { // take in account nested names
            ClassNamed clazz = this.findFirstFile(name.substring(0, dotIndex)).mainClass();
            return ClassNamed.of(
                clazz.packageName(),
                clazz.simpleName(),
                name.substring(dotIndex + 1).split("\\.")
            );
        }

        return this.findFirstFile(name).mainClass();
    }

    public SourceFile findFirstFile(String name) {
        try (Stream<SourceFile> stream = this.findFile(name)) { // handle conflict?
            return stream.findFirst().orElseThrow();
        }
    }

    public Stream<SourceFile> findFile(String name) {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/" + name + ".java");
        try {
            return Files.find(this.base, this.maxDepth, (path, attributes) -> matcher.matches(path))
                .map(finalPath -> SourceFile.of(this.base.relativize(finalPath)));
        } catch (IOException ex) {
            LOGGER.warn("I/O error occurred while trying to find a valid class name for {}", name, ex);
            throw new RuntimeException(ex);
        }
    }
}
