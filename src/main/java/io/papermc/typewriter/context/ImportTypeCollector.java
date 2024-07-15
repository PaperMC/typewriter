package io.papermc.typewriter.context;

import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.parser.StringReader;
import io.papermc.typewriter.utils.Formatting;
import org.jetbrains.annotations.VisibleForTesting;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

    private String getShortName0(ClassNamed type, Set<String> imports, Set<String> globalImports, boolean fetchStatic) {
        ClassNamed foundClass = type;
        int advancedNode = 0;
        while (!imports.contains(foundClass.canonicalName()) &&
            !globalImports.contains(foundClass.enclosing().canonicalName())) {
            if (foundClass.isRoot() || // top classes with package check is handled before
                (fetchStatic && !Modifier.isStatic(foundClass.knownClass().getModifiers())) // static imports are allowed for regular class too but only when the inner classes are all static
            ) {
                foundClass = null;
                break;
            }
            foundClass = foundClass.enclosing();
            advancedNode++;
        }
        if (foundClass != null) {
            String typeName;
            if (advancedNode > 0) { // direct inner class import
                String originalNestedName = type.dottedNestedName();
                int skipNode = Formatting.countOccurrences(originalNestedName, '.') - advancedNode;
                StringReader reader = new StringReader(originalNestedName);
                while (skipNode > 0) {
                    reader.skipStringUntil('.');
                    reader.skip(); // skip dot
                    skipNode--;
                }
                typeName = reader.getRemaining();
            } else {
                typeName = type.simpleName();
            }

            return typeName;
        }

        return type.canonicalName();
    }

    @Override
    public String getShortName(ClassNamed type) {
        return this.typeCache.computeIfAbsent(type, key -> {
            if (key.packageName().equals(JAVA_LANG_PACKAGE)) { // auto-import
                return key.dottedNestedName();
            }

            if (key.knownClass() != null && Modifier.isStatic(key.knownClass().getModifiers())) {
                // this is only supported when the class is known for now but generally static imports should stick to member of class not the class itself
                String name = getShortName0(key, this.staticImports.keySet(), this.globalStaticImports, true);
                if (!name.equals(key.canonicalName())) {
                    return name;
                }
            }

            if ((key.enclosing().isRoot() && this.globalImports.contains(key.packageName())) ||  // star import on package for top classes and one level classes only!
                (key.isRoot() && key.packageName().equals(this.rewriteClass.packageName()))) {  // same package don't need fqn too for top classes
                return key.dottedNestedName();
            }

            // self classes (with inner classes)
            // todo rework this logic order should be smth like: root stuff -> regular import -> inner stuff (-> static import if valid)
            // and remove the implicit part too
            Set<String> currentImports = this.imports;
            if (key.packageName().equals(this.rewriteClass.packageName()) &&
                this.rewriteClass.root().equals(key.root())) {
                int depth = Formatting.countOccurrences(key.dottedNestedName(), '.');
                int fromDepth = Formatting.countOccurrences(this.rewriteClass.dottedNestedName(), '.');
                if (fromDepth < depth) {
                    ClassNamed parent = key;
                    while (true) {
                        ClassNamed up = parent.enclosing();
                        if (this.rewriteClass.equals(up)) {
                            break;
                        }
                        parent = up;
                    }
                    currentImports = new HashSet<>(this.imports);
                    currentImports.add(parent.canonicalName()); // implicit import
                } else {
                    return type.simpleName();
                }
            }

            return getShortName0(key, currentImports, this.globalImports, false);
        });
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
