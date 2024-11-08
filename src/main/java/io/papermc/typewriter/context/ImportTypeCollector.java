package io.papermc.typewriter.context;

import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.ImportLayout;
import io.papermc.typewriter.parser.name.ProtoImportTypeName;
import io.papermc.typewriter.parser.token.TokenType;
import org.jetbrains.annotations.VisibleForTesting;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ImportTypeCollector implements ImportCollector {

    private static final String JAVA_LANG_PACKAGE = "java.lang";
    private static final String IMPORT_ON_DEMAND_MARKER = TokenType.STAR.name;

    private final Map<ClassNamed, String> typeCache = new HashMap<>();

    private final Set<String> imports = new LinkedHashSet<>();
    private final Set<String> globalImports = new LinkedHashSet<>();

    private final Map<String, String> staticImports = new LinkedHashMap<>(); // <fqn.id:id>
    private final Set<String> globalStaticImports = new LinkedHashSet<>();

    private final Set<ImportTypeName> addedImports = new LinkedHashSet<>();

    private final ClassNamed mainClass;
    private boolean modified;

    public ImportTypeCollector(ClassNamed mainClass) {
        this.mainClass = mainClass;
    }

    @Override
    public void addImport(String typeName) {
        final boolean changed;
        final String formattedName;
        boolean isGlobal = typeName.endsWith(IMPORT_ON_DEMAND_MARKER);

        if (isGlobal) {
            formattedName = typeName.substring(0, typeName.lastIndexOf(ProtoImportTypeName.IDENTIFIER_SEPARATOR));
            changed = this.globalImports.add(formattedName);
        } else {
            formattedName = typeName;
            changed = this.imports.add(typeName);
        }

        if (changed) {
            this.addedImports.add(new ImportTypeName(formattedName, isGlobal, false, true));
            this.modified = true;
        }
    }

    @Override
    public void addStaticImport(String fullName) {
        final boolean changed;
        final String formattedName;
        boolean isGlobal = fullName.endsWith(IMPORT_ON_DEMAND_MARKER);

        if (isGlobal) {
            formattedName = fullName.substring(0, fullName.lastIndexOf(ProtoImportTypeName.IDENTIFIER_SEPARATOR));
            changed = this.globalStaticImports.add(formattedName);
        } else {
            formattedName = fullName;
            changed = this.staticImports.put(formattedName, fullName.substring(fullName.lastIndexOf(ProtoImportTypeName.IDENTIFIER_SEPARATOR) + 1)) == null;
        }

        if (changed) {
            this.addedImports.add(new ImportTypeName(formattedName, isGlobal, true, true));
            this.modified = true;
        }
    }

    public void addProtoImport(ProtoImportTypeName typeName) {
        // assume type name is valid (checkIntegrity + read time check)
        if (!typeName.isStatic()) {
            Set<String> imports = typeName.isGlobal() ? this.globalImports : this.imports;
            imports.add(typeName.getTypeName());
        } else {
            if (typeName.isGlobal()) {
                this.globalStaticImports.add(typeName.getTypeName());
            } else {
                this.staticImports.put(typeName.getTypeName(), typeName.getStaticMemberName());
            }
        }
        this.addedImports.add(new ImportTypeName(typeName.getTypeName(), typeName.isGlobal(), typeName.isStatic(), false));
    }

    // only check conflict, duplicate imports (with import on demand type) are not checked but the file should compile
    @Override
    public boolean canImportSafely(ClassNamed type) {
        for (String importType : this.imports) {
            String identifier = importType.substring(importType.lastIndexOf(ProtoImportTypeName.IDENTIFIER_SEPARATOR) + 1);
            if (type.simpleName().equals(identifier)) {
                return false;
            }
        }

        // while this is not always required it ensure clarity of the source file
        for (String identifier : this.staticImports.values()) {
            if (type.simpleName().equals(identifier)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getStaticMemberShortName(String fullName) {
        if (this.staticImports.containsKey(fullName)) {
            return this.staticImports.get(fullName);
        }

        // global imports
        int lastDotIndex = fullName.lastIndexOf(ProtoImportTypeName.IDENTIFIER_SEPARATOR);
        if (lastDotIndex == -1) {
            return fullName;
        }

        String parentCanonicalName = fullName.substring(0, lastDotIndex);
        if (this.globalStaticImports.contains(parentCanonicalName)) {
            return fullName.substring(lastDotIndex + 1);
        }
        return fullName;
    }

    private Optional<String> getShortName0(ClassNamed type, Set<String> imports, Set<String> globalImports, boolean unusualStaticImport) {
        ClassNamed foundClass = type;
        ClassNamed upperClass = type.enclosing();
        while (foundClass != null) {
            if (unusualStaticImport && !Modifier.isStatic(foundClass.knownClass().getModifiers())) {
                // static imports are allowed for regular class too but only when the inner classes are all static
                return Optional.empty();
            }

            if (imports.contains(foundClass.canonicalName()) ||
                (upperClass != null && globalImports.contains(upperClass.canonicalName()))) {
                break;
            }

            foundClass = upperClass;
            if (upperClass != null) {
                upperClass = upperClass.enclosing();
            }
        }

        if (foundClass != null) {
            return Optional.of(type.dottedNestedName().substring(foundClass.dottedNestedName().length() - foundClass.simpleName().length()));
        }

        if (!unusualStaticImport && globalImports.contains(type.packageName())) { // star import on package
            return Optional.of(type.dottedNestedName());
        }

        return Optional.empty();
    }

    @Override
    public String getShortName(ClassNamed type, boolean autoImport) {
        return this.typeCache.computeIfAbsent(type, key -> {
            Optional<String> shortName = getShortName0(key, this.imports, this.globalImports, false); // regular import
            if (shortName.isEmpty() && key.knownClass() != null && Modifier.isStatic(key.knownClass().getModifiers())) {
                // this is only supported when the class is known for now but generally static imports should stick to member of class not the class itself
                shortName = getShortName0(key, this.staticImports.keySet(), this.globalStaticImports, true);
            }

            return shortName.orElseGet(() -> {
                // import have priority over those implicit things
                if (key.packageName().equals(JAVA_LANG_PACKAGE) || // auto-import
                    key.packageName().equals(this.mainClass.packageName()) // same package don't need fqn too
                ) {
                    return key.dottedNestedName();
                }

                if (autoImport) {
                    ClassNamed topType = type.topLevel();
                    if (this.canImportSafely(topType)) {
                        this.addImport(topType.canonicalName()); // only import top level, nested class are rarely imported directly
                        return type.dottedNestedName();
                    }
                }

                return key.canonicalName();
            });
        });
    }

    public String writeImports(ImportLayout.Section layout) {
        StringBuilder builder = new StringBuilder();
        List<ImportTypeName> addedImports = new ArrayList<>(this.addedImports);
        layout.sortImportsInto(builder, addedImports);
        return builder.toString();
    }

    public boolean isModified() {
        return this.modified;
    }

    @VisibleForTesting
    public ImportSet getImports() {
        return ImportSet.from(this.imports, this.globalImports);
    }

    @VisibleForTesting
    public ImportSet getStaticImports() {
        return ImportSet.from(this.staticImports.keySet(), this.globalStaticImports);
    }
}
