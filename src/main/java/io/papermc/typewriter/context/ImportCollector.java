package io.papermc.typewriter.context;

import io.papermc.typewriter.ClassNamed;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import static io.papermc.typewriter.context.ImportName.dotJoin;

@DefaultQualifier(NonNull.class)
public interface ImportCollector {

    ImportCollector NO_OP = new ImportCollector() { // only used for dump

        @Override
        public void addSingleImport(ClassNamed type) {
        }

        @Override
        public void addImport(ImportCategory<? extends ImportName> category, String name) {
        }

        @Override
        public boolean canImportSafely(ClassNamed type) {
            return false;
        }

        @Override
        public String getStaticMemberShortName(String packageName, String memberName) {
            return dotJoin(packageName, memberName);
        }

        @Override
        public String getShortName(ClassNamed type, boolean autoImport) {
            return type.canonicalName();
        }
    };

    void addSingleImport(ClassNamed type);

    void addImport(ImportCategory<? extends ImportName> category, String name);

    boolean canImportSafely(ClassNamed type);

    String getStaticMemberShortName(String packageName, String memberName);

    default String getShortName(Class<?> type) {
        return this.getShortName(ClassNamed.of(type));
    }

    default String getShortName(ClassNamed type) {
        return this.getShortName(type, true);
    }

    default String getShortName(Class<?> type, boolean autoImport) {
        return this.getShortName(ClassNamed.of(type), autoImport);
    }

    String getShortName(ClassNamed type, boolean autoImport);
}
