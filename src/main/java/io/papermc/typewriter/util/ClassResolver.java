package io.papermc.typewriter.util;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
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

    private static @MonotonicNonNull ClassResolver runtime;

    public static ClassResolver atRuntime() { // only for class or system modules
        if (runtime == null) {
            ClassResolver resolver = new ClassResolver(ClassResolver.class.getClassLoader());
            runtime = resolver;
        }
        return runtime;
    }

    private final ClassLoader loader;

    public ClassResolver(Set<Path> classpath) {
        this(new URLClassLoader(classpath.stream().map(resource -> {
            try {
                return resource.toUri().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException("Malformed classpath for element: %s".formatted(resource), e);
            }
        }).toArray(URL[]::new), ClassResolver.class.getClassLoader()));
    }

    public ClassResolver(ClassLoader loader) {
        this.loader = loader;
    }

    public Optional<Class<?>> find(String name) {
        try {
            return Optional.of(this.loader.loadClass(name));
        } catch (ClassNotFoundException ignored) {
        }
        return Optional.empty();
    }
}
