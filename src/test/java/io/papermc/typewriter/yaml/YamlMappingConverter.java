package io.papermc.typewriter.yaml;

import org.junit.jupiter.params.converter.TypedArgumentConverter;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;

public class YamlMappingConverter<T> extends TypedArgumentConverter<String, T> {

    private static final LoaderOptions OPTIONS;
    static {
        OPTIONS = new LoaderOptions();
        OPTIONS.setNestingDepthLimit(3);
    }

    private final Constructor yamlConstructor;

    protected YamlMappingConverter(Class<T> clazz) {
        super(String.class, clazz);
        this.yamlConstructor = new Constructor(clazz, OPTIONS);
    }

    @Override
    protected T convert(String path) {
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream(path)) {
            return new Yaml(this.yamlConstructor).load(input);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
