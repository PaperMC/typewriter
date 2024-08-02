package io.papermc.typewriter.context;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.parser.name.ImportTypeName;
import io.papermc.typewriter.parser.token.TokenType;
import org.jetbrains.annotations.VisibleForTesting;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ImportTypeCollector implements ImportCollector {

    private static final String JAVA_LANG_PACKAGE = "java.lang";
    private static final String IMPORT_ON_DEMAND_MARKER = TokenType.STAR.name;

    private final Map<ClassNamed, String> typeCache = new HashMap<>();

    private final Set<String> imports = new HashSet<>();
    private final Set<String> globalImports = new HashSet<>();

    private final Map<String, String> staticImports = new HashMap<>(); // <fqn.id:id>
    private final Set<String> globalStaticImports = new HashSet<>();

    private final ClassNamed mainClass;
    private ClassNamed accessSource;

    public ImportTypeCollector(ClassNamed mainClass) {
        this.mainClass = mainClass;
        this.accessSource = mainClass;
    }

    @Override
    public void setAccessSource(ClassNamed accessSource) {
        Preconditions.checkArgument(accessSource == null || accessSource.topLevel().equals(this.mainClass), "Access source must be a nested class of " + this.mainClass.canonicalName());
        this.accessSource = accessSource == null ? this.mainClass : accessSource;
    }

    @Override
    public void addImport(String typeName) {
        if (typeName.endsWith(IMPORT_ON_DEMAND_MARKER)) {
            this.globalImports.add(typeName.substring(0, typeName.lastIndexOf(ImportTypeName.IDENTIFIER_SEPARATOR)));
        } else {
            this.imports.add(typeName);
        }
    }

    @Override
    public void addStaticImport(String fullName) {
        if (fullName.endsWith(IMPORT_ON_DEMAND_MARKER)) {
            this.globalStaticImports.add(fullName.substring(0, fullName.lastIndexOf(ImportTypeName.IDENTIFIER_SEPARATOR)));
        } else {
            this.staticImports.put(fullName, fullName.substring(fullName.lastIndexOf(ImportTypeName.IDENTIFIER_SEPARATOR) + 1));
        }
    }

    public void addProtoImport(ImportTypeName typeName) {
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
    }

    @Override
    public String getStaticMemberShortName(String fullName) {
        if (this.staticImports.containsKey(fullName)) {
            return this.staticImports.get(fullName);
        }

        // global imports
        int lastDotIndex = fullName.lastIndexOf(ImportTypeName.IDENTIFIER_SEPARATOR);
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
    public String getShortName(ClassNamed type) {
        return this.typeCache.computeIfAbsent(type, key -> {
            Optional<String> shortName = getShortName0(key, this.imports, this.globalImports, false); // regular import
            if (shortName.isEmpty() && key.knownClass() != null && Modifier.isStatic(key.knownClass().getModifiers())) {
                // this is only supported when the class is known for now but generally static imports should stick to member of class not the class itself
                shortName = getShortName0(key, this.staticImports.keySet(), this.globalStaticImports, true);
            }

            // self classes (with inner classes)
            if (this.mainClass.equals(key.topLevel())) {
                int importedSize = shortName.map(String::length).orElse(0);
                String innerName = getInnerShortName(this.accessSource, key);
                if (importedSize == 0 || innerName.length() < importedSize) {
                    // inner name might be shorter than self import
                    return innerName;
                }
            }

            return shortName.orElseGet(() -> {
                // import have priority over those implicit things
                if (key.packageName().equals(JAVA_LANG_PACKAGE) || // auto-import
                    key.packageName().equals(this.mainClass.packageName()) // same package don't need fqn too
                ) {
                    return key.dottedNestedName();
                }
                return key.canonicalName();
            });
        });
    }

    public String getInnerShortName(ClassNamed fromType, ClassNamed targetType) {
        int targetSize = targetType.dottedNestedName().length();
        int fromSize = fromType.dottedNestedName().length();

        int maxOffset = Math.min(fromSize, targetSize - 1);
        int startOffset;
        for (startOffset = 0; startOffset < maxOffset; startOffset++) {
            if (fromType.dottedNestedName().charAt(startOffset) != targetType.dottedNestedName().charAt(startOffset)) {
                break;
            }
        }

        if (targetSize > fromSize && targetType.dottedNestedName().charAt(startOffset) == ImportTypeName.IDENTIFIER_SEPARATOR) {
            startOffset++;
        }

        return targetType.dottedNestedName().substring(startOffset);
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
