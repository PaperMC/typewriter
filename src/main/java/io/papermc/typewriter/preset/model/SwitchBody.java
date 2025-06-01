package io.papermc.typewriter.preset.model;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.context.IndentUnit;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SwitchBody implements CodeEmitter {

    private List<SwitchCases> cases = new ArrayList<>();
    private @Nullable SwitchCases defaultCase;

    private SwitchBody(Collection<SwitchCases> branches) {
        this.then(branches);
    }

    @Contract(value = "_ -> new", pure = true)
    public static SwitchBody of(SwitchCases... branches) {
        return of(Arrays.asList(branches));
    }

    @Contract(value = "_ -> new", pure = true)
    public static SwitchBody of(Collection<SwitchCases> branches) {
        return new SwitchBody(branches);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public SwitchBody then(SwitchCases... branches) {
        return then(Arrays.asList(branches));
    }

    @Contract(value = "_ -> this", mutates = "this")
    public SwitchBody then(Collection<SwitchCases> branches) {
        Preconditions.checkArgument(branches.stream().noneMatch(SwitchCases::isDefault), "These switch cases cannot contains default one!");
        this.cases.addAll(branches);
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public SwitchBody withDefault(SwitchCases defaultCase) {
        Preconditions.checkArgument(defaultCase.isDefault(), "This switch case is not a default one!");
        this.defaultCase = defaultCase;
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public SwitchBody withDefault(CodeBlock body) {
        return withDefault(SwitchCases.ofDefault(body, false));
    }

    public void mergeSimilarBranches(Comparator<String> keySort) {
        List<SwitchCases> cases = new ArrayList<>();

        List<SwitchCases> nonDefaultCases = new ArrayList<>();
        Map<CodeBlock, SwitchCases.SwitchCasesChain> stats = new HashMap<>();
        @Nullable CodeBlock defaultValue = this.defaultCase == null ? null : this.defaultCase.body();

        // merge similar cases, and omit cases similar to the default
        for (SwitchCases branch : this.cases) {
            CodeBlock body = branch.body();

            if (defaultValue == null || !Objects.equals(branch.body(), defaultValue)) {
                nonDefaultCases.add(branch);

                final SwitchCases.SwitchCasesChain newCases;
                if (stats.containsKey(body)) {
                    newCases = stats.get(body);
                } else {
                    newCases = SwitchCases.chain();
                    newCases.sortLabels(keySort);
                }
                newCases.addAll(Objects.requireNonNull(branch.labels()));

                stats.put(body, newCases);
            }
        }

        // keep the initial configuration
        boolean inlined = true;
        boolean arrow = true;
        for (SwitchCases chain : nonDefaultCases) {
            if (!chain.inlined()) {
                inlined = arrow = false;
                break;
            }
            if (!chain.arrow()) {
                arrow = false;
            }
        }

        // complete builders with collected infos
        for (Map.Entry<CodeBlock, SwitchCases.SwitchCasesChain> stat : stats.entrySet()) {
            SwitchCases.SwitchCasesChain branch = stat.getValue();

            if (inlined) {
                branch.inlined(arrow);
            }
            cases.add(branch.body(stat.getKey()).build());
        }
        this.cases = cases;
    }

    @Override
    public void emitCode(String indent, IndentUnit indentUnit, StringBuilder builder) {
        for (SwitchCases cases : this.cases) {
            cases.emitCode(indent, indentUnit, builder);
        }

        if (this.defaultCase != null) {
            this.defaultCase.emitCode(indent, indentUnit, builder);
        }
    }
}
