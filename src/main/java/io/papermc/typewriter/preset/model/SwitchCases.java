package io.papermc.typewriter.preset.model;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.IndentUnit;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record SwitchCases(@Nullable List<String> values, CodeBlock content, boolean inlined, boolean arrow) implements CodeEmitter {

    public SwitchCases(@Nullable List<String> values, CodeBlock content, boolean inlined, boolean arrow) {
        if (values == null) {
            Preconditions.checkArgument(inlined, "Default case must be inlined");
        } else {
            Preconditions.checkArgument(!values.isEmpty(), "Values cannot be empty in a regular case!");
        }

        this.values = values == null ? null : List.copyOf(values);
        this.content = content;
        this.inlined = inlined;
        this.arrow = arrow;
    }

    public boolean isDefault() {
        return this.values == null;
    }

    @Contract(value = "-> new", pure = true)
    public static SwitchCasesChain chain() {
        return new SwitchCasesChain();
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static SwitchCases of(String value, CodeBlock content) {
        return new SwitchCases(Collections.singletonList(value), content, false, false);
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    public static SwitchCases inlined(String value, CodeBlock content, boolean arrow) {
        return new SwitchCases(Collections.singletonList(value), content, true, arrow);
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static SwitchCases ofDefault(CodeBlock content, boolean arrow) {
        return new SwitchCases(null, content, true, arrow);
    }

    @Override
    public void emitCode(String indent, IndentUnit indentUnit, StringBuilder builder) {
        boolean needCurlyBrackets = false;
        if (this.values != null && !this.inlined) {
            for (String value : this.values) {
                builder.append(indent).append("case ").append(value).append(':');
                builder.append('\n');
            }
        } else {
            if (this.values != null) {
                builder.append(indent).append("case ").append(String.join(", ", this.values));
            } else {
                builder.append(indent).append("default");
            }

            if (this.arrow) {
                builder.append(" -> ");
                if (this.content.codeLines().size() > 1) {
                    needCurlyBrackets = true;
                }
            } else {
                builder.append(':');
            }

            if (needCurlyBrackets) {
                builder.append('{');
            }
            builder.append('\n');
        }

        this.content.emitCode(indent + indentUnit.content(), indentUnit, builder);

        if (needCurlyBrackets) {
            builder.append(indent).append('}');
            builder.append('\n');
        }
    }

    public static final class SwitchCasesChain {

        private final Set<String> values = new HashSet<>();
        private @Nullable Comparator<? super String> comparator;
        private @MonotonicNonNull CodeBlock content;
        private boolean inlined;
        private boolean arrow;

        private SwitchCasesChain() {
        }

        @Contract(value = "_ -> this", mutates = "this")
        public SwitchCasesChain add(String value) {
            this.values.add(value);
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public SwitchCasesChain addAll(String... values) {
            return addAll(Arrays.asList(values));
        }

        @Contract(value = "_ -> this", mutates = "this")
        public SwitchCasesChain addAll(Collection<String> values) {
            this.values.addAll(values);
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public SwitchCasesChain sortValues(Comparator<? super String> comparator) {
            this.comparator = comparator;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public SwitchCasesChain inlined(boolean arrow) {
            this.inlined = true;
            this.arrow = arrow;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public SwitchCasesChain enclosingContent(String line) {
            return enclosingContent(CodeBlock.of(line));
        }

        @Contract(value = "_ -> this", mutates = "this")
        public SwitchCasesChain enclosingContent(CodeBlock content) {
            this.content = content;
            return this;
        }

        @Contract(value = "-> new", pure = true)
        public SwitchCases build() {
            List<String> values = new ArrayList<>(this.values);
            if (this.comparator != null) {
                values.sort(this.comparator);
            }
            return new SwitchCases(List.copyOf(values), this.content, this.inlined, this.arrow);
        }
    }
}
