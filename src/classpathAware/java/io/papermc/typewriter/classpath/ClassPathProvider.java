package io.papermc.typewriter.classpath;

import io.github.classgraph.ClassGraph;

import java.net.URI;
import java.util.List;

@SuppressWarnings("unused") // used by ImportNameCollector reflectively
public final class ClassPathProvider {

    private ClassPathProvider() {}

    public static final List<URI> CLASS_PATH = new ClassGraph().getClasspathURIs();
}
