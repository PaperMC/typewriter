package io.papermc.typewriter.yaml;

import java.util.Map;

public class ShortNameMapping {

    public Map<String, String> shortNames;
    public Map<String, String> memberShortNames;

    /**
     * Note: unlike {@link #getMemberShortNames()} (which only use a dot), nested class must be identified using '$' since the typeName is then
     * converted into a class object
     */
    public Map<String, String> getShortNames() {
        return this.shortNames;
    }

    public Map<String, String> getMemberShortNames() {
        return this.memberShortNames;
    }
}
