package io.papermc.typewriter.context;

import io.papermc.typewriter.ClassNamed;

public interface ImportCollector {

    ImportCollector NO_OP = new ImportCollector() { // only used for dump

        @Override
        public void addImport(String typeName) {
        }

        @Override
        public void addStaticImport(String fullName) {
        }

        @Override
        public boolean canImportSafely(ClassNamed type) {
            return false;
        }

        @Override
        public String getStaticMemberShortName(String fullName) {
            return fullName;
        }

        @Override
        public String getShortName(ClassNamed type, boolean autoImport) {
            return type.canonicalName();
        }
    };

    void addImport(String typeName);

    void addStaticImport(String fullName);

    boolean canImportSafely(ClassNamed type);

    String getStaticMemberShortName(String fullName);

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
