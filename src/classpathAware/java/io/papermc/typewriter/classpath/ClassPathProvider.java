package io.papermc.typewriter.classpath;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.net.URI;
import java.util.List;

@SuppressWarnings("unused") // used by ClassGraphBridge reflectively
public final class ClassPathProvider {

    private ClassPathProvider() {}

    public static final List<URI> CLASS_PATH = new ClassGraph().getClasspathURIs();

    public static String getModuleName(String className) {
        try (ScanResult scan = new ClassGraph().acceptClasses(className).scan()) {
            ClassInfo info = scan.getClassInfo(className);
            if (info == null) {
                return null;
            }
            String name = info.getModuleInfo().getName();
            return name.isEmpty() ? null : name;
        }
    }
}
