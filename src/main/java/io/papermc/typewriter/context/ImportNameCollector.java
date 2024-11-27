package io.papermc.typewriter.context;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.parser.name.ProtoImportName;
import javax.lang.model.SourceVersion;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class ImportNameCollector implements ImportCollector {

    private static final String JAVA_LANG_PACKAGE = "java.lang";

    private final Map<ClassNamed, String> typeCache = new HashMap<>();
    private final ImportNameMap importMap = new ImportNameMap();

    private final ClassNamed mainClass;
    private boolean modified;

    public ImportNameCollector(ClassNamed mainClass) {
        this.mainClass = mainClass;
    }

    @Override
    public void addImport(ClassNamed type) {
        if (this.importMap.add(new ImportName.Type(type))) {
            this.modified = true;
        }
    }

    @Override
    public void addImport(String name) {
        if (this.importMap.add(ImportName.Type.fromQualifiedName(name))) {
            this.modified = true;
        }
    }

    @Override
    public void addStaticImport(String name) {
        if (this.importMap.add(ImportName.Static.fromQualifiedMemberName(name))) {
            this.modified = true;
        }
    }

    public void addProtoImport(ProtoImportName proto) {
        // assume type name is valid
        if (proto.isStatic()) {
            this.importMap.add(new ImportName.Static(proto.getName(), proto.getStaticMemberName(), proto.isGlobal(), false));
        } else {
            this.importMap.add(new ImportName.Type(proto.getName(), proto.isGlobal(), false));
        }
    }

    // only check conflict, duplicate imports (with import on demand type) are not checked but the file should compile
    @Override
    public boolean canImportSafely(ClassNamed type) {
        return this.importMap.canImportSafely(type);
    }

    @Override
    public String getStaticMemberShortName(String packageName, String memberName) {
        Preconditions.checkArgument(packageName.isEmpty() || SourceVersion.isName(packageName), "Access name is not a valid name");
        Preconditions.checkArgument(SourceVersion.isName(memberName), "Member name is not a valid name");
        return this.importMap.getStaticMemberName(packageName, memberName);
    }

    private Optional<String> getShortName0(ClassNamed type, ImportCategory category) {
        Set<ImportName> imports = this.importMap.get(category);
        ClassNamed foundClass = type;
    loop:
        while (foundClass != null) {
            if (category == ImportCategory.STATIC && !Modifier.isStatic(foundClass.knownClass().getModifiers())) {
                // static imports are allowed for regular class too but only when the inner classes are all static
                return Optional.empty();
            }

            Supplier<ClassNamed> enclosing = Suppliers.memoize(foundClass::enclosing);
            for (ImportName importName : imports) {
                if (importName.isImported(foundClass, enclosing)) {
                    break loop;
                }
            }

            foundClass = enclosing.get();
        }

        if (foundClass != null) {
            return Optional.of(type.dottedNestedName().substring(foundClass.dottedNestedName().length() - foundClass.simpleName().length()));
        }

        return Optional.empty();
    }

    @Override
    public String getShortName(ClassNamed type, boolean autoImport) {
        return this.typeCache.computeIfAbsent(type, key -> {
            Optional<String> shortName = getShortName0(key, ImportCategory.TYPE); // regular import
            if (shortName.isEmpty() && key.knownClass() != null && Modifier.isStatic(key.knownClass().getModifiers())) {
                // this is only supported when the class is known for now
                shortName = getShortName0(key, ImportCategory.STATIC);
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
                    if (this.importMap.canImportSafely(topType)) {
                        this.addImport(topType.canonicalName()); // only import top level, nested class are rarely imported directly
                        return type.dottedNestedName();
                    }
                }

                return key.canonicalName();
            });
        });
    }

    public String writeImports(ImportLayout.Header header) {
        StringBuilder builder = new StringBuilder();
        List<ImportName> addedImports = new ArrayList<>(this.importMap.entries());
        header.sortImportsInto(builder, addedImports);
        return builder.toString();
    }

    public boolean isModified() {
        return this.modified;
    }

    public ImportNameMap getImportMap() {
        return this.importMap;
    }
}
