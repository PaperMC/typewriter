package io.papermc.typewriter.util;

import io.papermc.typewriter.ClassNamed;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

@DefaultQualifier(NonNull.class)
public class ClassResolver {

    private final ClassLoader loader;

    public ClassResolver(Set<Path> classpath) {
        this(new URLClassLoader(classpath.stream().map(resource -> {
            try {
                return resource.toUri().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException("Malformed classpath: %s".formatted(classpath), e);
            }
        }).toArray(URL[]::new), ClassResolver.class.getClassLoader()));
    }

    public ClassResolver(ClassLoader loader) {
        this.loader = loader;
    }

    public Optional<Class<?>> tryFind(String name) {
        try {
            return Optional.of(this.loader.loadClass(name));
        } catch (ClassNotFoundException ignored) {
        }
        return Optional.empty();
    }

    public ClassNamed find(String name) {
        return this.tryFind(name).map(ClassNamed::new).orElseThrow();
    }

    public ClassNamed resolve(ClassNamed unresolved) {
        if (unresolved.knownClass() != null) {
            return unresolved;
        }

        return this.tryFind(unresolved.binaryName()).map(ClassNamed::new).orElse(unresolved);
    }

    public ClassNamed resolveOrThrow(ClassNamed unresolved) {
        if (unresolved.knownClass() != null) {
            return unresolved;
        }

        return this.tryFind(unresolved.binaryName()).map(ClassNamed::new).orElseThrow();
    }
}
