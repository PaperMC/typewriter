package io.papermc.typewriter.util;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.lang.module.FindException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@DefaultQualifier(NonNull.class)
public class ClassResolver {

    private static @MonotonicNonNull ClassResolver runtime;

    public static ClassResolver atRuntime() { // only for class or system modules
        if (runtime == null) {
            ClassResolver resolver = new ClassResolver(ClassResolver.class.getClassLoader());
            resolver.finderFactory = ModuleFinder::ofSystem;
            runtime = resolver;
        }
        return runtime;
    }

    private final ClassLoader loader;
    private @Nullable ModuleFinder finder;
    private @Nullable Supplier<ModuleFinder> finderFactory;

    public ClassResolver(Set<Path> classpath) {
        this(new URLClassLoader(classpath.stream().map(resource -> {
            try {
                return resource.toUri().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException("Malformed classpath for element: %s".formatted(resource), e);
            }
        }).toArray(URL[]::new), ClassResolver.class.getClassLoader()));
        this.finderFactory = classpath.isEmpty() ? null :
            () -> ModuleFinder.compose(ModuleFinder.ofSystem(), ModuleFinder.of(classpath.toArray(Path[]::new)));
    }

    private ClassResolver(ClassLoader loader) {
        this.loader = loader;
    }

    public Optional<Class<?>> find(String name) {
        try {
            return Optional.of(this.loader.loadClass(name));
        } catch (ClassNotFoundException ignored) {
        }
        return Optional.empty();
    }

    public Optional<ModuleReference> findModule(String name) {
        if (this.finderFactory == null) {
            return Optional.empty();
        }

        if (this.finder == null) { // init
            this.finder = this.finderFactory.get();
        }

        try {
            return this.finder.find(name);
        } catch (FindException ignored) {
            this.finder = finderFactory.get();
        }
        return Optional.empty();
    }
}
