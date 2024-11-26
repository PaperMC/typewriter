package io.papermc.typewriter.preset.model;

import io.papermc.typewriter.context.IndentUnit;
import io.papermc.typewriter.parser.StringReader;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class CodeBlock implements CodeEmitter {

    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\R");

    private final List<String> lines;
    private @Nullable IndentTokens indentTokens;

    private CodeBlock(List<String> lines) {
        this.lines = List.copyOf(lines);
    }

    private CodeBlock(List<String> lines, @Nullable IndentTokens indentTokens) {
        this(lines);
        this.indentTokens = indentTokens;
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static CodeBlock from(IndentUnit indentUnit, String content) {
        return from(indentUnit, LINE_BREAK_PATTERN.split(content, -1));
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static CodeBlock from(IndentUnit indentUnit, String... lines) {
        List<String> newlines = new ArrayList<>(lines.length);
        @Nullable IndentTokens indentTokens = new IndentTokens();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            StringReader reader = new StringReader(line);

            int level = 0;
            while (reader.trySkipChars(indentUnit.size(), indentUnit.character())) {
                level++;
            }

            final String newline;
            if (reader.getCursor() == 0) {
                newline = line;
            } else {
                newline = reader.getRemaining();
            }

            newlines.add(newline);
            indentTokens.setLevel(i, level);
        }

        if (indentTokens.isEmpty()) {
            indentTokens = null;
        }

        return new CodeBlock(newlines, indentTokens);
    }

    // support $> $< to indent the code using provided indent unit
    @Contract(value = "_ -> new", pure = true)
    public static CodeBlock format(String content) {
        return format(LINE_BREAK_PATTERN.split(content, -1));
    }

    @Contract(value = "_ -> new", pure = true)
    public static CodeBlock format(String... lines) {
        ArrayList<String> newlines = new ArrayList<>(lines.length);
        int level = 0;
        @Nullable IndentTokens indentTokens = new IndentTokens();
        for (final String line : lines) {
            StringReader reader = new StringReader(line);
            while (reader.canRead(2) && reader.peek() == '$') {
                char c = reader.peek(1);
                if (c == '>' || c == '<') {
                    if (c == '>') {
                        level++;
                    } else {
                        level--;
                    }
                    reader.setCursor(reader.getCursor() + 2);
                } else {
                    break;
                }
            }

            if (level < 0) {
                throw new IllegalStateException("Cannot remove one level of indentation further. This might happens if you have more '$<' than '$>' for a given line.");
            }

            final String newline;
            if (reader.getCursor() == 0) {
                newline = line;
            } else {
                newline = reader.getRemaining();
                if (newline.isEmpty()) {
                    continue; // improve readability, line is ignored after the token to have more space
                }
            }

            newlines.add(newline);
            indentTokens.setLevel(newlines.size() - 1, level);
        }

        newlines.trimToSize();
        if (indentTokens.isEmpty()) {
            indentTokens = null;
        }

        return new CodeBlock(newlines, indentTokens);
    }

    @Contract(value = "_ -> new", pure = true)
    public static CodeBlock of(String content) {
        return of(LINE_BREAK_PATTERN.split(content, -1));
    }

    @Contract(value = "_ -> new", pure = true)
    public static CodeBlock of(String... lines) {
        return new CodeBlock(Arrays.asList(lines));
    }

    public List<String> lines() {
        return this.lines;
    }

    // ignore blank lines
    public List<String> fullLines() {
        return this.lines.stream().filter(line -> !line.trim().isEmpty()).toList();
    }

    @Override
    public void emitCode(String indent, IndentUnit indentUnit, StringBuilder builder) {
        for (int i = 0, size = this.lines.size(); i < size; i++) {
            final String line = this.lines.get(i);

            if (!line.isEmpty()) { // empty line doesn't need indentation (this could be configurable)
                builder.append(indent);
                if (this.indentTokens != null) {
                    builder.append(indentUnit.content().repeat(this.indentTokens.getLevel(i)));
                }
                builder.append(line);
            }

            builder.append('\n');
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null || o.getClass() != this.getClass()) return false;
        CodeBlock other = (CodeBlock) o;
        return this.lines.equals(other.lines());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.lines);
    }
}
