package io.papermc.typewriter.context;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.context.layout.ImportHeader;
import io.papermc.typewriter.context.layout.ImportScheme;
import io.papermc.typewriter.parser.name.ProtoImportName;
import javax.lang.model.SourceVersion;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.io.File;
import java.lang.module.FindException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class ImportNameCollector implements ImportCollector {

    private static final String JAVA_LANG_PACKAGE = "java.lang";
    private static @MonotonicNonNull Supplier<ModuleFinder> finderFactory;
    static {
        initModuleFinderFactory();
    }

    private final Map<ClassNamed, String> typeCache = new HashMap<>();
    private final ImportNameMap importMap = new ImportNameMap();

    private final ClassNamed mainClass;
    private ModuleFinder finder;
    private boolean modified;

    public ImportNameCollector(ClassNamed mainClass) {
        this.mainClass = mainClass;
    }

    private static void initModuleFinderFactory() {
        String fullPaths = System.getProperty("jdk.module.path", System.getProperty("jdk.module.upgrade.path"));
        final ModuleFinder local;
        if (fullPaths != null) {
            local = ModuleFinder.of(Arrays.stream(fullPaths.split(File.pathSeparator)).map(Path::of).toArray(Path[]::new));
        } else {
            local = null;
        }
        ModuleFinder system = ModuleFinder.ofSystem();
        finderFactory = () -> local == null ? system : ModuleFinder.compose(system, local);
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

    @Override
    public void addModuleImport(String name) {
        if (this.importMap.add(new ImportName.Module(name, true))) {
            this.modified = true;
        }
    }

    public void addProtoImport(ProtoImportName proto) {
        // assume type name is valid
        ImportCategory<?> category = proto.getCategory();
        if (category == ImportCategory.STATIC) {
            this.importMap.add(new ImportName.Static(proto.getName(), proto.getStaticMemberName(), proto.isGlobal(), false));
        } else if (category == ImportCategory.TYPE) {
            this.importMap.add(new ImportName.Type(proto.getName(), proto.isGlobal(), false));
        } else if (category == ImportCategory.MODULE) {
            this.importMap.add(new ImportName.Module(proto.getName(), false));
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

    private boolean isImported(ImportName.Identified importName, ClassNamed klass, Supplier<ClassNamed> enclosingKlass) {
        if (importName.isGlobal()) {
            ClassNamed enclosing = enclosingKlass.get();
            final String parentName = enclosing != null ? enclosing.canonicalName() : klass.packageName(); // handle package import
            return ImportName.asGlobal(parentName).equals(importName.name());
        }

        return klass.canonicalName().equals(importName.name());
    }

    private boolean isPackageExported(ImportName.Module module, ClassNamed klass) {
        if (this.finder == null) {
            this.finder = finderFactory.get();
        }

        Optional<ModuleReference> reference = Optional.empty();
        try {
            reference = this.finder.find(module.name());
        } catch (FindException ignored) {
            this.finder = finderFactory.get();
        }

        if (reference.isPresent()) {
            for (String pkg : reference.get().descriptor().packages()) {
                if (pkg.equals(klass.packageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private <T extends ImportName> Optional<String> getShortName0(ClassNamed type, ImportCategory<T> category) {
        Set<T> imports = this.importMap.get(category);
        ClassNamed foundClass = type;

    loop:
        while (foundClass != null) {
            if (category == ImportCategory.STATIC && !Modifier.isStatic(foundClass.knownClass().getModifiers())) {
                // static imports are allowed for regular class too but only when the inner classes are all static
                return Optional.empty();
            }

            Supplier<ClassNamed> enclosing = Suppliers.memoize(foundClass::enclosing);
            if (category != ImportCategory.MODULE) {
                for (T importName : imports) {
                    if (this.isImported((ImportName.Identified) importName, foundClass, enclosing)) {
                        break loop;
                    }
                }
            } else {
                for (T importName : imports) {
                    if (this.isPackageExported((ImportName.Module) importName, foundClass)) {
                        break loop;
                    }
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
            if (shortName.isEmpty()) {
                shortName = getShortName0(key, ImportCategory.MODULE);
            }
            if (shortName.isEmpty() && key.knownClass() != null && Modifier.isStatic(key.knownClass().getModifiers())) {
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
                    ClassNamed topType = key.topLevel();
                    if (this.importMap.canImportSafely(topType)) {
                        this.addImport(topType.canonicalName()); // only import top level, nested class are rarely imported directly
                        return key.dottedNestedName();
                    }
                }

                return key.canonicalName();
            });
        });
    }

    private void printImportStatement(StringBuilder builder, ImportName type) {
        builder.append("import ");
        type.category().identity().ifPresent(name -> {
            builder.append(name).append(' ');
        });
        builder.append(type.name());
        builder.append(';');
    }

    public String writeImports(ImportHeader header) {
        Set<ImportName> addedImports = this.importMap.entries();
        StringBuilder builder = new StringBuilder();
        for (Iterator<ImportScheme.Item> iterator = header.scheme().view().iterator(); iterator.hasNext(); ) {
            ImportScheme.Item item = iterator.next();
            List<ImportName> types = addedImports.stream()
                .filter(item::contains)
                .sorted(header.order().comparator)
                .toList();

            if (!types.isEmpty()) {
                builder.append("\n".repeat(item.previousSpace()));
                for (ImportName type : types) {
                    this.printImportStatement(builder, type);
                    if (iterator.hasNext()) {
                        builder.append('\n');
                    }
                }
                builder.append("\n".repeat(item.nextSpace()));
            }
        }

        return builder.toString();
    }

    public boolean isModified() {
        return this.modified;
    }

    public ImportNameMap getImportMap() {
        return this.importMap;
    }
}
