package io.papermc.typewriter.preset.model;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

class IndentTokens {

    private final NavigableMap<Integer, Integer> tokens = new TreeMap<>();
    private int level;

    void setLevel(int line, int level) {
        if (this.level != level) {
            this.tokens.put(line, level);
            this.level = level;
        }
    }

    int getLevel(int line) {
        Map.Entry<Integer, Integer> entry = this.tokens.floorEntry(line);
        if (entry == null) {
            return 0; // no tokens, initial level
        }
        return entry.getValue();
    }

    boolean isEmpty() {
        return this.tokens.isEmpty();
    }
}
