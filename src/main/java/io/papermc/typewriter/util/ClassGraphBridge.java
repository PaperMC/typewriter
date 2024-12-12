package io.papermc.typewriter.util;

import io.papermc.typewriter.ClassNamed;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@ApiStatus.Internal
public class ClassGraphBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassGraphBridge.class);

    private static VarHandle CLASS_GRAPH_PATHS;
    private static MethodHandle CLASS_GRAPH_MODULE_NAME;
    static {
        Class<?> provider;
        try {
            provider = Class.forName("io.papermc.typewriter.classpath.ClassPathProvider");
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            CLASS_GRAPH_PATHS = lookup.findStaticVarHandle(provider, "CLASS_PATH", List.class);
            CLASS_GRAPH_MODULE_NAME = lookup.findStatic(provider, "getModuleName", MethodType.methodType(String.class, String.class));
            LOGGER.debug("Using classgraph extension!");
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static @Nullable ModuleFinder runtimeFinder() {
        Path[] paths = null;
        if (CLASS_GRAPH_PATHS != null) {
            List<URI> classPath = (List<URI>) CLASS_GRAPH_PATHS.get();
            if (!classPath.isEmpty()) {
                paths = classPath.stream().map(Path::of).toArray(Path[]::new);
            }
        } else {
            String fullPaths = System.getProperty("jdk.module.path", System.getProperty("java.class.path"));
            if (fullPaths != null) {
                paths = Arrays.stream(fullPaths.split(File.pathSeparator)).map(Path::of).toArray(Path[]::new);
            }
        }
        return paths == null ? null : ModuleFinder.of(paths);
    }

    // should probably be done once for all registered classes
    public static String getModuleName(ClassNamed mainClass) {
        if (CLASS_GRAPH_MODULE_NAME != null) {
            try {
                return (String) CLASS_GRAPH_MODULE_NAME.invokeExact(mainClass.canonicalName());
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }

        if (mainClass.knownClass() == null) {
            return null;
        }

        Module module = mainClass.knownClass().getModule();
        if (module.isNamed()) {
            return module.getName();
        }

        try {
            ModuleFinder finder = ModuleFinder.of(Path.of(mainClass.knownClass().getProtectionDomain().getCodeSource().getLocation().toURI()));
            for (ModuleReference reference : finder.findAll()) {
                ModuleDescriptor descriptor = reference.descriptor();
                if (descriptor.packages().contains(mainClass.packageName())) {
                    return descriptor.name();
                }
            }
        } catch (URISyntaxException ignored) {
        }
        return null;
    }
}
