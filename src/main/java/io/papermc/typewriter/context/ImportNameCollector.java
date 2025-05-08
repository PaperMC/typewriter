package io.papermc.typewriter.context;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.context.layout.ImportHeader;
import io.papermc.typewriter.context.layout.ImportScheme;
import io.papermc.typewriter.parser.Keywords;
import io.papermc.typewriter.parser.name.ProtoImportName;
import io.papermc.typewriter.util.ClassHelper;
import io.papermc.typewriter.util.ClassResolver;
import javax.lang.model.SourceVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReference;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class ImportNameCollector implements ImportCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportNameCollector.class);
    private static final String JAVA_LANG_PACKAGE = "java.lang";

    private final Map<ClassNamed, String> typeCache = new HashMap<>();
    private final ImportNameMap importMap = new ImportNameMap();

    private final ClassNamed mainClass;
    private final ClassResolver resolver;
    private final Supplier<String> targetModuleName;
    private boolean modified;

    public ImportNameCollector(ClassNamed mainClass, ClassResolver resolver) {
        this.mainClass = mainClass;
        this.resolver = resolver;
        this.targetModuleName = Suppliers.memoize(() -> ClassHelper.getModuleName(Objects.requireNonNull(mainClass.resolve(resolver).reference())));
    }

    @Override
    public void addSingleImport(ClassNamed type) {
        if (this.importMap.add(new ImportName.Type(type))) {
            this.modified = true;
        }
    }

    @Override
    public void addImport(ImportCategory<? extends ImportName> category, String name) {
        if (this.importMap.add(category.parse(name))) {
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
        for (ImportName.Type name : this.importMap.get(ImportCategory.TYPE)) {
            if (name.isGlobal()) {
                continue;
            }

            if (type.simpleName().equals(name.id())) {
                return false;
            }
        }

        // while this is not always required it ensure clarity of the source file
        for (ImportName.Static name : this.importMap.get(ImportCategory.STATIC)) {
            if (name.isGlobal()) {
                continue;
            }

            if (type.simpleName().equals(name.id())) {
                return false;
            }
        }

        return true;
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

    private boolean isPackageExported(String moduleName, String packageName) {
        Optional<ModuleReference> referenceOpt = this.resolver.findModule(moduleName);
        if (referenceOpt.isPresent()) {
            ModuleDescriptor descriptor = referenceOpt.get().descriptor();
            for (ModuleDescriptor.Exports export : descriptor.exports()) {
                String targetModuleName = this.targetModuleName.get();
                if (targetModuleName != null && export.isQualified() && !export.targets().contains(targetModuleName)) {
                    continue;
                }

                if (export.source().equals(packageName)) {
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
            if (category == ImportCategory.STATIC && !Modifier.isStatic(foundClass.reference().getModifiers())) {
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
            if (shortName.isEmpty() && key.reference() != null && Modifier.isStatic(key.reference().getModifiers())) {
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
                    if (this.canImportSafely(topType)) {
                        this.addSingleImport(topType); // only import top level, nested class are rarely imported directly
                        return key.dottedNestedName();
                    }
                }

                return key.canonicalName();
            });
        });
    }

    private void printImportStatement(StringBuilder builder, ImportName type) {
        builder.append(Keywords.IMPORT);
        builder.append(" ");
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

            LOGGER.error("Some imports have been skipped since the import layout is not defined for them!");
            remainingImports.forEach(name -> {
                StringBuilder log = new StringBuilder();
                log.append("- ");
                printImportStatement(log, name);
                LOGGER.error(log.toString());
            });
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
