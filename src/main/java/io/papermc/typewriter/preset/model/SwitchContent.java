package io.papermc.typewriter.preset.model;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.IndentUnit;
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

public class SwitchContent implements CodeEmitter {

    private List<SwitchCases> cases = new ArrayList<>();
    private @Nullable SwitchCases defaultCase;

    private SwitchContent(Collection<SwitchCases> branches) {
        this.then(branches);
    }

    @Contract(value = "_ -> new", pure = true)
    public static SwitchContent of(SwitchCases... branches) {
        return of(Arrays.asList(branches));
    }

    @Contract(value = "_ -> new", pure = true)
    public static SwitchContent of(Collection<SwitchCases> branches) {
        return new SwitchContent(branches);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public SwitchContent then(SwitchCases... branches) {
        return then(Arrays.asList(branches));
    }

    @Contract(value = "_ -> this", mutates = "this")
    public SwitchContent then(Collection<SwitchCases> branches) {
        Preconditions.checkArgument(branches.stream().noneMatch(SwitchCases::isDefault), "These switch cases cannot contains default one!");
        this.cases.addAll(branches);
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public SwitchContent withDefault(SwitchCases defaultCase) {
        Preconditions.checkArgument(defaultCase.isDefault(), "This switch case is not a default one!");
        this.defaultCase = defaultCase;
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public SwitchContent withDefault(CodeBlock content) {
        return withDefault(SwitchCases.ofDefault(content, false));
    }

    public void mergeSimilarBranches(Comparator<String> keySort) {
        List<SwitchCases> cases = new ArrayList<>();

        List<SwitchCases> nonDefaultCases = new ArrayList<>();
        Map<CodeBlock, SwitchCases.SwitchCasesChain> handleObjects = new HashMap<>();
        @Nullable CodeBlock defaultValue = this.defaultCase == null ? null : this.defaultCase.content();

        // merge similar cases, and omit cases similar to the default
        for (SwitchCases branch : this.cases) {
            CodeBlock content = branch.content();

            if (defaultValue == null || !Objects.equals(branch.content(), defaultValue)) {
                nonDefaultCases.add(branch);

                final SwitchCases.SwitchCasesChain newCases;
                if (handleObjects.containsKey(content)) {
                    newCases = handleObjects.get(content);
                } else {
                    newCases = SwitchCases.chain();
                    newCases.sortValues(keySort);
                }
                newCases.addAll(Objects.requireNonNull(branch.values()));

                handleObjects.put(content, newCases);
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
        for (Map.Entry<CodeBlock, SwitchCases.SwitchCasesChain> entry : handleObjects.entrySet()) {
            SwitchCases.SwitchCasesChain branch = entry.getValue();

            if (inlined) {
                branch.inlined(arrow);
            }
            cases.add(branch.enclosingContent(entry.getKey()).build());
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
