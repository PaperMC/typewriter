package io.papermc.typewriter.context;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.context.layout.ImportLayout;
import io.papermc.typewriter.context.layout.ImportScheme;
import io.papermc.typewriter.parser.Keywords;
import io.papermc.typewriter.parser.name.ProtoImportName;
import javax.lang.model.SourceVersion;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@DefaultQualifier(NonNull.class)
public class ImportNameCollector implements ImportCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportNameCollector.class);
    private static final String JAVA_LANG_PACKAGE = "java.lang";

    private final Map<ClassNamed, String> typeCache = new HashMap<>();
    private final ImportNameMap importMap = new ImportNameMap();

    private final ClassNamed mainClass;
    private boolean modified;

    public ImportNameCollector(ClassNamed mainClass) {
        this.mainClass = mainClass;
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
            this.importMap.add(new ImportName.Static(proto.getName(), proto.getStaticMemberName(), proto.isWilddcard(), false));
        } else if (category == ImportCategory.TYPE) {
            this.importMap.add(new ImportName.Type(proto.getName(), proto.isWilddcard(), false));
        }
    }

    // only check conflict, duplicate imports (with import on demand type) are not checked but the file should compile
    @Override
    public boolean canImportSafely(ClassNamed type) {
        for (ImportName.Type name : this.importMap.get(ImportCategory.TYPE)) {
            if (name.isWildcard()) {
                continue;
            }

            if (type.simpleName().equals(name.id())) {
                return false;
            }
        }

        // while this is not always required it ensure clarity of the source file
        for (ImportName.Static name : this.importMap.get(ImportCategory.STATIC)) {
            if (name.isWildcard()) {
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

    private boolean isUsed(ImportName.Identified importName, ClassNamed klass, Supplier<@Nullable ClassNamed> enclosingKlass) {
        if (importName.isWildcard()) {
            @Nullable ClassNamed enclosing = enclosingKlass.get();
            final String parentName = enclosing != null ? enclosing.canonicalName() : klass.packageName(); // handle package import
            return ImportName.asWildcard(parentName).equals(importName.name());
        }

        return klass.canonicalName().equals(importName.name());
    }

    private <T extends ImportName.Identified> Optional<String> getShortName0(ClassNamed type, ImportCategory<T> category) {
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

            Supplier<@Nullable ClassNamed> enclosing = Suppliers.memoize(foundClass::enclosing);
            for (T importName : imports) {
                if (this.isUsed(importName, foundClass, enclosing)) {
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
            if (shortName.isEmpty() && key.reference() != null && Modifier.isStatic(key.reference().getModifiers())) {
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

    public String writeImports(ImportLayout layout) {
        Set<ImportName> remainingImports = new HashSet<>(this.importMap.entries());
        StringBuilder builder = new StringBuilder();
        loop:
        for (ImportScheme.Item item : layout.scheme()) {
            List<ImportName> types = remainingImports.stream()
                .filter(item::contains)
                .sorted(layout.order().comparator)
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
            LOGGER.warn("Some imports don't have a defined import layout: {}", remainingImports);
            builder.append('\n');
            Iterator<ImportName> types = remainingImports.iterator();
            while (types.hasNext()) {
                ImportName type = types.next();
                types.remove();
                this.printImportStatement(builder, type);
                if (types.hasNext()) {
                    builder.append('\n');
                }
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
