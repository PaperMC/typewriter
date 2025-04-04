package io.papermc.typewriter.util;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.SourceFile;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A class named view search through a specified source set path a
 * java source file and convert it to {@link ClassNamed} if possible.
 * <br>
 * Do note that the class is not known at runtime though.
 * <br>
 * The max depth represents the number of sub folders
 * taken into account.
 */
@DefaultQualifier(NonNull.class)
public class ClassNamedView {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassNamedView.class);
    private final Path base;
    private final Path sourceSet;
    private final int maxDepth;

    public ClassNamedView(Path sourceSet, int maxDepth, @Nullable String base) {
        Preconditions.checkArgument(Files.isDirectory(sourceSet), "Source set path must point to a directory");
        this.sourceSet = sourceSet;
        this.maxDepth = maxDepth;
        this.base = base == null ? sourceSet : sourceSet.resolve(base);
    }

    private ClassNamedView(Path sourceSet, int maxDepth, Path base) {
        this.sourceSet = sourceSet;
        this.maxDepth = maxDepth;
        this.base = base;
    }

    public ClassNamedView subView(String relativePath) {
        Path newBase = this.base.resolve(relativePath);
        int deltaDepth = newBase.getNameCount() - this.base.getNameCount();
        Preconditions.checkArgument(deltaDepth <= this.maxDepth, "Target path is too nested");
        return new ClassNamedView(this.sourceSet, this.maxDepth - deltaDepth, newBase);
    }

    public ClassNamed findFirst(String name) {
        return this.tryFindFirst(name).orElseThrow();
    }

    public Optional<ClassNamed> tryFindFirst(String name) {
        int dotIndex = name.indexOf('.');
        if (dotIndex != -1) { // take in account nested names
            return this.findFirstFile(name.substring(0, dotIndex))
                .map(SourceFile::mainClass)
                .map(clazz -> ClassNamed.of(
                    clazz.packageName(),
                    clazz.simpleName(),
                    name.substring(dotIndex + 1).split("\\.")
                ));
        }

        return this.findFirstFile(name).map(SourceFile::mainClass);
    }

    public Stream<ClassNamed> find(String name) {
        int dotIndex = name.indexOf('.');
        final String fileName;
        @Nullable String nestedClassNames;
        if (dotIndex != -1) { // take in account nested names
            fileName = name.substring(0, dotIndex);
            nestedClassNames = name.substring(dotIndex + 1);
        } else {
            fileName = name;
            nestedClassNames = null;
        }

        return this.findFile(fileName)
            .map(file -> {
            if (nestedClassNames != null) {
                ClassNamed clazz = file.mainClass();
                return ClassNamed.of(
                    clazz.packageName(),
                    clazz.simpleName(),
                    nestedClassNames
                );
            }

            return file.mainClass();
        });
    }

    public Optional<SourceFile> findFirstFile(String name) {
        try (Stream<SourceFile> stream = this.findFile(name)) {
            return stream.findFirst();
        }
    }

    public Stream<SourceFile> findFile(String name) {
        try {
            return Files.find(this.base, this.maxDepth, (path, attributes) -> attributes.isRegularFile() && path.endsWith(name + ".java"))
                .map(finalPath -> SourceFile.of(this.sourceSet.relativize(finalPath)));
        } catch (IOException ex) {
            LOGGER.warn("I/O error occurred while trying to find a valid class name for {}", name, ex);
            throw new RuntimeException(ex);
        }
    }
}
