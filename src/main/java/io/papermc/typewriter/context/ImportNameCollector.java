package io.papermc.typewriter.context;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.context.layout.ImportHeader;
import io.papermc.typewriter.context.layout.ImportScheme;
import io.papermc.typewriter.parser.name.ProtoImportName;
import javax.lang.model.SourceVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Supplier;

public class ImportNameCollector implements ImportCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportNameCollector.class);
    private static final String JAVA_LANG_PACKAGE = "java.lang";

    private static Supplier<ModuleFinder> finderFactory;
    private static ModuleFinder finder;

    private static VarHandle CLASS_GRAPH_PATHS;
    static {
        Class<?> provider;
        try {
            provider = Class.forName("io.papermc.typewriter.classpath.ClassPathProvider");
            CLASS_GRAPH_PATHS = MethodHandles.lookup().findStaticVarHandle(provider, "CLASS_PATH", List.class);
        } catch (ClassNotFoundException ignored) {
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
        initModuleFinderFactory();
    }

    private final Map<ClassNamed, String> typeCache = new HashMap<>();
    private final ImportNameMap importMap = new ImportNameMap();

    private final ClassNamed mainClass;
    private boolean modified;

    public ImportNameCollector(ClassNamed mainClass) {
        this.mainClass = mainClass;
    }

    private static void initModuleFinderFactory() {
        Path[] paths = null;
        if (CLASS_GRAPH_PATHS != null) {
            LOGGER.debug("Using classgraph extension!");
            List<URI> classPath = (List<URI>) CLASS_GRAPH_PATHS.get();
            if (!classPath.isEmpty()) {
                paths = classPath.stream().map(Path::of).toArray(Path[]::new);
            }
        } else {
            String fullPaths = System.getProperty("jdk.module.path", System.getProperty("java.class.path"));
            if (fullPaths != null) {
                paths = Arrays.stream(fullPaths.split(File.pathSeparator)).map(Path::of).toArray(Path[]::new);
            }
        }
        ModuleFinder system = ModuleFinder.ofSystem();
        ModuleFinder runtime = paths == null ? null : ModuleFinder.of(paths);
        finderFactory = () -> runtime == null ? system : ModuleFinder.compose(system, runtime);
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
        if (this.importMap.add(ImportName.Module.fromQualifiedName(name))) {
            this.modified = true;
        }
    }

    public void addProtoImport(ProtoImportName proto) {
        // assume name is valid
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

    private boolean isUsed(ImportName.Identified importName, ClassNamed klass, Supplier<ClassNamed> enclosingKlass) {
        if (importName.isGlobal()) {
            ClassNamed enclosing = enclosingKlass.get();
            final String parentName = enclosing != null ? enclosing.canonicalName() : klass.packageName(); // handle package import
            return ImportName.asGlobal(parentName).equals(importName.name());
        }

        return klass.canonicalName().equals(importName.name());
    }

    private boolean isPackageExported(String moduleName, String packageName) { // todo unnamed module? valid too for import declaration
        if (finder == null) {
            finder = finderFactory.get();
        }

        Optional<ModuleReference> referenceOpt = Optional.empty();
        try {
            referenceOpt = finder.find(moduleName);
        } catch (FindException ignored) {
            finder = finderFactory.get();
        }

        if (referenceOpt.isPresent()) {
            ModuleDescriptor descriptor = referenceOpt.get().descriptor();
            for (ModuleDescriptor.Exports exports : descriptor.exports()) {
                if (exports.isQualified() && !exports.targets().contains(this.mainClass.packageName())) {
                    continue;
                }

                if (exports.source().equals(packageName)) {
                    return true;
                }
            }

            for (ModuleDescriptor.Requires require : descriptor.requires()) {
                if (require.modifiers().contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE)) {
                    if (isPackageExported(require.name(), packageName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isUsed(ImportName.Module module, ClassNamed klass) {
        return isPackageExported(module.name(), klass.packageName());
    }

    @FunctionalInterface
    interface ImportUsed<T extends ImportName> {
        boolean test(T name, ClassNamed klass, Supplier<ClassNamed> enclosingKlass);
    }

    private <T extends ImportName> Optional<String> getShortName0(ClassNamed type, ImportCategory<T> category, ImportUsed<T> isUsed) {
        Set<T> imports = this.importMap.get(category);
        if (imports.isEmpty()) {
            return Optional.empty();
        }

        ClassNamed foundClass = type;
    loop:
        while (foundClass != null) {
            if (category == ImportCategory.STATIC && !Modifier.isStatic(foundClass.knownClass().getModifiers())) {
                // static imports are allowed for regular class too but only when the inner classes are all static
                return Optional.empty();
            }

            Supplier<ClassNamed> enclosing = Suppliers.memoize(foundClass::enclosing);
            for (T importName : imports) {
                if (isUsed.test(importName, foundClass, enclosing)) {
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
            Optional<String> shortName = getShortName0(key, ImportCategory.TYPE, this::isUsed); // regular import
            if (shortName.isEmpty()) {
                shortName = getShortName0(key, ImportCategory.MODULE, (name, klass, enclosingKlass) -> isUsed(name, klass));
            }
            if (shortName.isEmpty() && key.knownClass() != null && Modifier.isStatic(key.knownClass().getModifiers())) {
                shortName = getShortName0(key, ImportCategory.STATIC, this::isUsed);
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

    private String printImportStatement(ImportName type) {
        StringBuilder builder = new StringBuilder();
        printImportStatement(builder, type);
        return builder.toString();
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
        Set<ImportName> remainingImports = new HashSet<>(this.importMap.entries());
        StringBuilder builder = new StringBuilder();
    loop:
        for (ImportScheme.Item item : header.scheme().view()) {
            List<ImportName> types = remainingImports.stream()
                .filter(item::contains)
                .sorted(header.order().comparator)
                .toList();

            if (!types.isEmpty()) {
                builder.append("\n".repeat(item.previousSpace()));
                for (ImportName type : types) {
                    remainingImports.remove(type);
                    this.printImportStatement(builder, type);
                    if (!remainingImports.isEmpty()) {
                        builder.append('\n');
                    } else {
                        break loop;
                    }
                }
            }
        }

        if (!remainingImports.isEmpty()) {
            builder.deleteCharAt(builder.length() - 1);
            StringJoiner joiner = new StringJoiner("", "- ", "\n");
            remainingImports.forEach(name -> joiner.add(printImportStatement(name)));
            LOGGER.error("Some imports have been skipped since the import layout is not defined for them!\n{}", joiner.toString());
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
