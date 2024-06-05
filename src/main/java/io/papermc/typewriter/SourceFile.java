package io.papermc.typewriter;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.Contract;
import java.nio.file.Path;
import java.util.Iterator;

public record SourceFile(ClassNamed mainClass, Path path, IndentUnit indentUnit) {

    public SourceFile {
        Preconditions.checkArgument(mainClass.isRoot(), "Main class is not a root class!");
        Preconditions.checkArgument(path.getNameCount() > 0, "Path %s cannot be a root element", path);
        Preconditions.checkArgument(path.getFileName().toString().endsWith(".java"), "Path %s is not a Java source file", path);
        Preconditions.checkArgument(!path.isAbsolute(), "Path %s cannot be absolute", path);
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static SourceFile of(ClassNamed mainClass, IndentUnit indentUnit) {
        Path path = Path.of(
            mainClass.packageName().replace('.', '/'),
            mainClass.simpleName().concat(".java")
        );
        return new SourceFile(mainClass, path, indentUnit);
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static SourceFile of(Class<?> mainClass, IndentUnit indentUnit) {
        return of(new ClassNamed(mainClass), indentUnit);
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static SourceFile of(Path path, IndentUnit indentUnit) {
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
        ), path, indentUnit);
    }
}
