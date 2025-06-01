package io.papermc.typewriter.preset.model;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.context.IndentUnit;
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

public record SwitchCases(@Nullable List<String> labels, CodeBlock body, boolean inlined, boolean arrow) implements CodeEmitter {

    public SwitchCases(@Nullable List<String> labels, CodeBlock body, boolean inlined, boolean arrow) {
        if (labels == null) {
            Preconditions.checkArgument(inlined, "Default case must be inlined");
        } else {
            Preconditions.checkArgument(!labels.isEmpty(), "Labels cannot be empty in a regular case!");
        }

        this.labels = labels == null ? null : List.copyOf(labels);
        this.body = body;
        this.inlined = inlined;
        this.arrow = arrow;
    }

    public boolean isDefault() {
        return this.labels == null;
    }

    @Contract(value = "-> new", pure = true)
    public static SwitchCasesChain chain() {
        return new SwitchCasesChain();
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static SwitchCases of(String label, CodeBlock body) {
        return new SwitchCases(Collections.singletonList(label), body, false, false);
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    public static SwitchCases inlined(String label, CodeBlock body, boolean arrow) {
        return new SwitchCases(Collections.singletonList(label), body, true, arrow);
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static SwitchCases ofDefault(CodeBlock body, boolean arrow) {
        return new SwitchCases(null, body, true, arrow);
    }

    @Override
    public void emitCode(String indent, IndentUnit indentUnit, StringBuilder builder) {
        boolean needCurlyBrackets = false;
        boolean indentBody = true;
        if (this.labels != null && !this.inlined) {
            for (String value : this.labels) {
                builder.append(indent).append("case ").append(value).append(':');
                builder.append('\n');
            }
        } else {
            if (this.labels != null) {
                builder.append(indent).append("case ").append(String.join(", ", this.labels));
            } else {
                builder.append(indent).append("default");
            }

            if (this.arrow) {
                builder.append(" -> ");
                if (this.body.fullLines().size() > 1) {
                    needCurlyBrackets = true;
                }
            } else {
                builder.append(':');
            }

            if (needCurlyBrackets) {
                builder.append('{');
            }
            if (needCurlyBrackets || !this.arrow) {
                builder.append('\n');
            } else {
                indentBody = false;
            }
        }

        this.body.emitCode(indentBody ? indent + indentUnit.content() : "", indentUnit, builder);

        if (needCurlyBrackets) {
            builder.append(indent).append('}');
            builder.append('\n');
        }
    }

    public static final class SwitchCasesChain {

        private final Set<String> labels = new HashSet<>();
        private @Nullable Comparator<? super String> comparator;
        private @MonotonicNonNull CodeBlock body;
        private boolean inlined;
        private boolean arrow;

        private SwitchCasesChain() {
        }

        @Contract(value = "_ -> this", mutates = "this")
        public SwitchCasesChain add(String label) {
            this.labels.add(label);
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public SwitchCasesChain addAll(String... labels) {
            return addAll(Arrays.asList(labels));
        }

        @Contract(value = "_ -> this", mutates = "this")
        public SwitchCasesChain addAll(Collection<String> labels) {
            this.labels.addAll(labels);
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public SwitchCasesChain sortLabels(Comparator<? super String> comparator) {
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
        public SwitchCasesChain body(String line) {
            return body(CodeBlock.of(line));
        }

        @Contract(value = "_ -> this", mutates = "this")
        public SwitchCasesChain body(CodeBlock body) {
            this.body = body;
            return this;
        }

        @Contract(value = "-> new", pure = true)
        public SwitchCases build() {
            Preconditions.checkState(this.body != null, "Switch case must have a defined content.");
            List<String> labels = new ArrayList<>(this.labels);
            if (this.comparator != null) {
                labels.sort(this.comparator);
            }
            return new SwitchCases(labels, this.body, this.inlined, this.arrow);
        }
    }
}
