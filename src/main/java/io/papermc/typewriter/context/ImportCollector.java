package io.papermc.typewriter.context;

import io.papermc.typewriter.ClassNamed;

public interface ImportCollector {

    ImportCollector NO_OP = new ImportCollector() { // only used for dump

        @Override
        public void addImport(ClassNamed type) {
        }

        @Override
        public void addImport(String name) {
        }

        @Override
        public void addStaticImport(String name) {
        }

        @Override
        public boolean canImportSafely(ClassNamed type) {
            return false;
        }

        @Override
        public String getStaticMemberShortName(String packageName, String memberName) {
            return ImportName.dotJoin(packageName, memberName);
        }

        @Override
        public String getShortName(ClassNamed type, boolean autoImport) {
            return type.canonicalName();
        }
    };

    default void addImport(Class<?> type) {
        this.addImport(new ClassNamed(type));
    }

    void addImport(ClassNamed type);

    void addImport(String name);

    void addStaticImport(String name);

    boolean canImportSafely(ClassNamed type);

    String getStaticMemberShortName(String packageName, String memberName);

    default String getShortName(Class<?> type) {
        return this.getShortName(new ClassNamed(type));
    }

    default String getShortName(ClassNamed type) {
        return this.getShortName(type, true);
    }

    default String getShortName(Class<?> type, boolean autoImport) {
        return this.getShortName(new ClassNamed(type), autoImport);
    }

    String getShortName(ClassNamed type, boolean autoImport);
}
