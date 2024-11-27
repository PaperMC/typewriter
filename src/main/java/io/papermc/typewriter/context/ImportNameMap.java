package io.papermc.typewriter.context;

import io.papermc.typewriter.ClassNamed;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static io.papermc.typewriter.parser.name.ProtoQualifiedName.IDENTIFIER_SEPARATOR;

public class ImportNameMap {

    private final Set<ImportName> entries = new LinkedHashSet<>();
    private final Map<ImportCategory, Set<ImportName>> names = new EnumMap<>(ImportCategory.class);

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

    public Set<ImportName> get(ImportCategory category) {
        return this.names.getOrDefault(category, Collections.emptySet());
    }

    public boolean canImportSafely(ClassNamed type) {
        if (this.names.containsKey(ImportCategory.TYPE)) {
            for (ImportName name : this.names.get(ImportCategory.TYPE)) {
                if (name.isGlobal()) {
                    continue;
                }

                if (type.simpleName().equals(name.id())) {
                    return false;
                }
            }
        }

        // while this is not always required it ensure clarity of the source file
        if (this.names.containsKey(ImportCategory.STATIC)) {
            for (ImportName name : this.names.get(ImportCategory.STATIC)) {
                if (name.isGlobal()) {
                    continue;
                }

                if (type.simpleName().equals(name.id())) {
                    return false;
                }
            }
        }

        return true;
    }

    public ImportSet asSet(ImportCategory category) {
        Set<String> single = new HashSet<>();
        Set<String> global = new HashSet<>();
        Set<ImportName> names = this.get(category);
        for (ImportName type : names) {
            if (type.isGlobal()) {
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
        Set<ImportName> names = this.get(ImportCategory.STATIC);
        while (true) {
            for (ImportName name : names) {
                ImportName.Static stat = (ImportName.Static) name;
                if (stat.isMemberImported(packageName, memberName)) {
                    return stat.resolveMemberName(packageName, originalName);
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
