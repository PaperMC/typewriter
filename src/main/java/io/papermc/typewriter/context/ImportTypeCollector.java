package io.papermc.typewriter.context;

import io.papermc.typewriter.ClassNamed;
import org.jetbrains.annotations.VisibleForTesting;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ImportTypeCollector implements ImportCollector {

    private static final String JAVA_LANG_PACKAGE = "java.lang";

    private final Map<ClassNamed, String> typeCache = new HashMap<>();

    private final Set<String> imports = new HashSet<>();
    private final Set<String> globalImports = new HashSet<>();

    private final Map<String, String> staticImports = new HashMap<>(); // <fqn.id:id>
    private final Set<String> globalStaticImports = new HashSet<>();

    private final ClassNamed rewriteClass;

    public ImportTypeCollector(ClassNamed rewriteClass) {
        this.rewriteClass = rewriteClass;
    }

    @Override
    public void addImport(String typeName) {
        if (typeName.endsWith("*")) {
            this.globalImports.add(typeName.substring(0, typeName.lastIndexOf('.')));
        } else {
            this.imports.add(typeName);
        }
    }

    @Override
    public void addStaticImport(String fullName) {
        if (fullName.endsWith("*")) {
            this.globalStaticImports.add(fullName.substring(0, fullName.lastIndexOf('.')));
        } else {
            this.staticImports.put(fullName, fullName.substring(fullName.lastIndexOf('.') + 1));
        }
    }

    @Override
    public String getStaticMemberShortName(String fullName) {
        if (this.staticImports.containsKey(fullName)) {
            return this.staticImports.get(fullName);
        }

        // global imports
        int lastDotIndex = fullName.lastIndexOf('.');
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
        while (!imports.contains(foundClass.canonicalName()) &&
            !globalImports.contains(foundClass.enclosing().canonicalName())) {
            if (foundClass.isRoot() ||
                (unusualStaticImport && !Modifier.isStatic(foundClass.knownClass().getModifiers())) // static imports are allowed for regular class too but only when the inner classes are all static
            ) {
                foundClass = null;
                break;
            }
            foundClass = foundClass.enclosing();
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

            return shortName.orElseGet(() -> {
                // import have priority over those implicit things
                if (key.packageName().equals(JAVA_LANG_PACKAGE)) { // auto-import
                    return key.dottedNestedName();
                }

                // self classes (with inner classes)
                if (this.rewriteClass.root().equals(key.root())) {
                    return getInnerShortName(this.rewriteClass, key);
                }

                if (key.packageName().equals(this.rewriteClass.packageName())) { // same package don't need fqn too
                    return key.dottedNestedName();
                }
                return key.canonicalName();
            });
        });
    }

    private String getInnerShortName(ClassNamed fromType, ClassNamed targetType) {
        if (fromType.dottedNestedName().equals(targetType.dottedNestedName()) || fromType.dottedNestedName().startsWith(targetType.dottedNestedName() + ".")) {
            return targetType.simpleName(); // reference self or enclosing class
        }

        Set<ClassNamed> visitedTypes = new HashSet<>();
        ClassNamed intersectType = null;
        ClassNamed type = targetType;

        while (!type.isRoot()) {
            if (type.dottedNestedName().equals(fromType.dottedNestedName())) {
                intersectType = type;
                break;
            }
            type = type.enclosing();
            visitedTypes.add(type);
        }

        if (intersectType == null && visitedTypes.contains(fromType)) {
            intersectType = fromType;
        }

        if (intersectType == null) {
            type = fromType;
            while (!type.isRoot()) {
                type = type.enclosing();
                if (visitedTypes.contains(type)) {
                    intersectType = type;
                    break;
                }
            }
        }

        if (intersectType == null) { // shouldn't happen (same root)
            return targetType.dottedNestedName();
        }

        return targetType.dottedNestedName().substring(intersectType.dottedNestedName().length() + 1); // reference sibling class
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
