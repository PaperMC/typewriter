package io.papermc.typewriter.yaml;

import io.papermc.typewriter.context.ImportSet;

import java.util.Collections;
import java.util.Set;

public class ImportSetBean implements ImportSet {

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
