package io.papermc.typewriter.util;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URISyntaxException;
import java.nio.file.Path;

public final class ClassHelper {

    public static Class<?> getTopLevelClass(Class<?> clazz) {
        Class<?> topLevelClass = clazz;
        Class<?> upperClass = clazz;
        while (true) {
            upperClass = upperClass.getEnclosingClass();
            if (upperClass == null) {
                break;
            }
            topLevelClass = upperClass;
        }
        return topLevelClass;
    }

    public static String retrieveFullNestedName(Class<?> clazz) {
        String fqn = clazz.getCanonicalName();
        String packageName = clazz.getPackageName();
        if (packageName.isEmpty()) {
            return fqn;
        }
        return fqn.substring(packageName.length() + 1);
    }

    public static String getModuleName(Class<?> clazz) {
        Module module = clazz.getModule();
        if (module.isNamed()) {
            return module.getName();
        }

        try {
            // not ideal...
            ModuleFinder finder = ModuleFinder.of(Path.of(clazz.getProtectionDomain().getCodeSource().getLocation().toURI()));
            for (ModuleReference reference : finder.findAll()) {
                ModuleDescriptor descriptor = reference.descriptor();
                if (descriptor.packages().contains(clazz.getPackageName())) {
                    return descriptor.name();
                }
            }
        } catch (URISyntaxException ignored) {
        }
        return null;
    }

    private ClassHelper() {
    }
}
