package io.papermc.typewriter.context;

import io.papermc.typewriter.ClassNamed;

public interface ImportCollector {

    ImportCollector NO_OP = new ImportCollector() {
        @Override
        public void addImport(final String typeName) {

        }

        @Override
        public void addStaticImport(final String fullName) {

        }

        @Override
        public String getStaticMemberShortName(final String fullName) {
            return fullName;
        }

        @Override
        public String getShortName(final ClassNamed type) {
            return type.canonicalName();
        }

    };

    void addImport(String typeName);

    void addStaticImport(String fullName);

    String getStaticMemberShortName(String fullName);

    default String getShortName(Class<?> type) {
        return this.getShortName(new ClassNamed(type));
    }

    String getShortName(ClassNamed type);

}
