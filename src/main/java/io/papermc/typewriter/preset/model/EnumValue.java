package io.papermc.typewriter.preset.model;

import io.papermc.typewriter.IndentUnit;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.Contract;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

public record EnumValue(String name, List<String> arguments, @Nullable CodeBlock body) implements CodeEmitter {

    public EnumValue(String name, List<String> arguments, @Nullable CodeBlock body) {
        this.name = name;
        this.arguments = List.copyOf(arguments);
        this.body = body;
    }

    @Contract(value = "_ -> new", pure = true)
    public static EnumValue value(String name) {
        return new EnumValue(name, Collections.emptyList(), null);
    }

    @Contract(value = "_ -> new", pure = true)
    public static Builder builder(String name) {
        return new Builder(name);
    }

    @Override
    public void emitCode(String indent, IndentUnit indentUnit, StringBuilder builder) {
        builder.append(indent).append(this.name);
        if (!this.arguments.isEmpty()) {
            builder.append('(').append(String.join(", ", this.arguments)).append(')');
        }

        if (this.body != null) {
            builder.append(" {");
            builder.append('\n');
            this.body.emitCode(indent + indentUnit.content(), indentUnit, builder);
            builder.append(indent).append('}');
        }
    }

    public static final class Builder {

        private final String name;
        private List<String> arguments = Collections.emptyList();
        private UnaryOperator<String> nameTransformer = name -> name;
        private @Nullable CodeBlock body;

        private Builder(String name) {
            this.name = name;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder argument(String argument) {
            return arguments(Collections.singletonList(argument));
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder arguments(String... arguments) {
            return arguments(Arrays.asList(arguments));
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder arguments(List<String> arguments) {
            this.arguments = arguments;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder rename(UnaryOperator<String> nameTransformer) {
            this.nameTransformer = nameTransformer;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder body(CodeBlock body) {
            this.body = body;
            return this;
        }

        @Contract(value = "-> new", pure = true)
        public EnumValue build() {
            return new EnumValue(this.nameTransformer.apply(this.name), this.arguments, this.body);
        }
    }
}
