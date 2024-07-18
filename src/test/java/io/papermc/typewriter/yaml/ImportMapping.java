package io.papermc.typewriter.yaml;

import java.util.Collections;
import java.util.Set;

public class ImportMapping {

    public ImportSet imports;
    public ImportSet staticImports;

    public ImportSet getImports() {
        return this.imports;
    }

    public ImportSet getStaticImports() {
        return this.staticImports;
    }

    public static class ImportSet implements io.papermc.typewriter.context.ImportSet {

        public Set<String> single;
        public Set<String> global;

        @Override
        public Set<String> single() {
            return this.single == null ? Collections.emptySet() : this.single;
        }

        @Override
        public Set<String> global() {
            return this.global == null ? Collections.emptySet() : this.global;
        }
    }
}
