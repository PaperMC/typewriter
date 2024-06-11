package io.papermc.typewriter.preset.model;

import io.papermc.typewriter.IndentUnit;
import io.papermc.typewriter.utils.Formatting;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.Contract;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public record EnumValue(String name, Collection<String> arguments, @Nullable CodeBlock body) implements CodeEmitter {

    public EnumValue(String name, Collection<String> arguments, @Nullable CodeBlock body) {
        this.name = name;
        this.arguments = List.copyOf(arguments);
        this.body = body;
    }

    @Contract(value = "_ -> new", pure = true)
    public static EnumValue value(String name) {
        return new EnumValue(name, Collections.emptyList(), null);
    }

    @Contract(value = "_ -> new", pure = true)
    public static EnumValue value(Object name) {
        return value(Formatting.asCode(name));
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
        private Collection<String> arguments = Collections.emptyList();
        private Function<String, String> nameTransformer = name -> name;
        private @Nullable CodeBlock body;

        private Builder(String name) {
            this.name = name;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder argument(String argument) {
            this.arguments = Collections.singletonList(argument);
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder arg(Object argument) {
            return argument(Formatting.asCode(argument));
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder arguments(String... arguments) {
            return arguments(Arrays.asList(arguments));
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder arguments(Collection<String> arguments) {
            this.arguments = arguments;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder args(Object... arguments) {
            return args(Arrays.asList(arguments));
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder args(Collection<Object> arguments) {
            return arguments(arguments.stream().map(Formatting::asCode).toList());
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder rename(Function<String, String> nameTransformer) {
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
