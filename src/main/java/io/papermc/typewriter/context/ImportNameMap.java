package io.papermc.typewriter.context;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static io.papermc.typewriter.parser.name.ProtoQualifiedName.IDENTIFIER_SEPARATOR;

public class ImportNameMap {

    private final Set<ImportName> entries = new LinkedHashSet<>();
    private final Map<ImportCategory<?>, Set<ImportName>> names = new IdentityHashMap<>();

    public boolean add(ImportName name) {
        if (this.entries.add(name)) {
            this.names.computeIfAbsent(name.category(), category -> new HashSet<>()).add(name);
            return true;
        }
        return false;
    }

    public boolean remove(ImportName name) {
        if (this.entries.remove(name)) {
            Set<ImportName> names = this.names.get(name.category());
            names.remove(name);
            if (names.isEmpty()) {
                this.names.remove(name.category());
            }
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public <T extends ImportName> Set<T> get(ImportCategory<T> category) {
        return (Set<T>) this.names.getOrDefault(category, Collections.emptySet());
    }

    public <T extends ImportName.Identified> ImportSet asSet(ImportCategory<T> category) {
        Set<String> single = new HashSet<>();
        Set<String> global = new HashSet<>();
        Set<T> names = this.get(category);
        for (T type : names) {
            if (type.isWildcard()) {
                global.add(type.name());
            } else {
                single.add(type.name());
            }
        }
        return ImportSet.from(single, global);
    }

    private @Nullable String parentName(String name) {
        int dotIndex = name.lastIndexOf(IDENTIFIER_SEPARATOR);
        if (dotIndex == -1) {
            return null;
        }
        return name.substring(0, dotIndex);
    }

    public String getStaticMemberName(String packageName, String memberName) {
        String fullName = ImportName.dotJoin(packageName, memberName);
        String originalName = memberName;
        Set<ImportName.Static> names = this.get(ImportCategory.STATIC);
        while (true) {
            for (ImportName.Static name : names) {
                if (name.isMemberImported(packageName, memberName)) {
                    return name.resolveMemberName(packageName, originalName);
                }
            }

            // check for inner name import (rarely used)
            String parent = parentName(memberName);
            if (parent == null) {
                break;
            }
            memberName = parent;
        }
        return fullName;
    }

    public Set<ImportName> entries() {
        return this.entries;
    }
}
