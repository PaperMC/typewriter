package io.papermc.typewriter.context;

import java.util.Collections;
import java.util.Set;

public interface ImportSet {

    Set<String> single();

    Set<String> global();

    static ImportSet from(Set<String> single, Set<String> global) {
        return new ImportSet() {
            @Override
            public Set<String> single() {
                return Collections.unmodifiableSet(single);
            }

            @Override
            public Set<String> global() {
                return Collections.unmodifiableSet(global);
            }
        };
    }
}
