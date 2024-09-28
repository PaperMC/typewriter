package io.papermc.typewriter;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.Contract;

import javax.swing.text.html.Option;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;

@DefaultQualifier(NonNull.class)
public record SourceFile(ClassNamed mainClass, Path path, Optional<IndentUnit> indentUnit) {

    public SourceFile {
        Preconditions.checkArgument(mainClass.isTopLevel(), "Main class is not a top level class!");
        Preconditions.checkArgument(path.getNameCount() > 0, "Path %s cannot be a root element", path);
        Preconditions.checkArgument(path.getFileName().toString().endsWith(".java"), "Path %s is not a Java source file", path);
        Preconditions.checkArgument(!path.isAbsolute(), "Path %s cannot be absolute", path);
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static SourceFile of(ClassNamed mainClass, @Nullable IndentUnit indentUnit) {
        Path path = Path.of(
            mainClass.packageName().replace('.', '/'),
            mainClass.simpleName().concat(".java")
        );
        return new SourceFile(mainClass, path, Optional.ofNullable(indentUnit));
    }

    @Contract(value = "_ -> new", pure = true)
    public static SourceFile of(ClassNamed mainClass) {
        return of(mainClass, null);
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static SourceFile of(Class<?> mainClass, @Nullable IndentUnit indentUnit) {
        return of(new ClassNamed(mainClass), indentUnit);
    }

    @Contract(value = "_ -> new", pure = true)
    public static SourceFile of(Class<?> mainClass) {
        return of(mainClass, null);
    }

    @Contract(value = "_ -> new", pure = true)
    public static SourceFile of(Path path) {
        return of(path, null);
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static SourceFile of(Path path, @Nullable IndentUnit indentUnit) {
        Preconditions.checkArgument(path.getNameCount() > 0, "Path %s cannot be a root element", path);

        StringBuilder packageName = new StringBuilder();
        Iterator<Path> parts = path.getParent().iterator();
        while (parts.hasNext()) {
            packageName.append(parts.next().toString());
            if (parts.hasNext()) {
                packageName.append('.');
            }
        }

        String name = path.getFileName().toString();
        int dotIndex = name.indexOf('.');
        return new SourceFile(ClassNamed.of(
            packageName.toString(),
            dotIndex == -1 ? name : name.substring(0, dotIndex)
        ), path, Optional.ofNullable(indentUnit));
    }
}
