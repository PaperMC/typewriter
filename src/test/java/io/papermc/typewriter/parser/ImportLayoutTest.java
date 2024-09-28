package io.papermc.typewriter.parser;

import io.papermc.typewriter.ImportLayout;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImportLayoutTest {

    private static final Path EDITOR_CONFIG = Path.of(System.getProperty("user.dir"), "src/testData/resources/.editorconfig");
    private static ImportLayout IMPORT_LAYOUT;

    private static Arguments layout(String target, String expectedLayout) {
        return Arguments.of(Path.of(target), expectedLayout);
    }

    private static Stream<Arguments> editorConfigTargets() {
        return Stream.of(
            layout("abc", "*,|,$*"),
            layout("/abc.java", "*,|,|,$*"),
            layout("sub/abc.java", "*,|,|,$*"),
            layout("Test1.java", "$*,|,java.**,|,*"),
            layout("Test2.java", "$*,|,java.**,|,*")
        );
    }

    @BeforeAll
    public static void setup() throws IOException {
        IMPORT_LAYOUT = ImportLayout.createFromEditorConfig(EDITOR_CONFIG);
    }

    @ParameterizedTest
    @MethodSource("editorConfigTargets")
    public void testReadLayout(Path target, String expectedLayout) {
        assertEquals(IMPORT_LAYOUT.getRelevantSection(target).layout(), expectedLayout);
    }
}
