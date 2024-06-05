package io.papermc.typewriter.parser.closure;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public enum ClosureType {
    PARENTHESIS("(", ")"),
    CURLY_BRACKET("{", "}"),
    BRACKET("[", "]"),
    COMMENT("/*", "*/"),
    PARAGRAPH("\"\"\"", "\"\"\""),
    STRING("\"", "\""),
    CHAR("'", "'");

    public final String start;
    public final String end;

    ClosureType(String start, String end) {
        this.start = start;
        this.end = end;
    }

    public boolean escapableByPreviousChar() {
        return ALLOW_ESCAPE.contains(this) && this.end.length() == 1 && this.start.equals(this.end);
    }

    private static final Set<ClosureType> ALLOW_ESCAPE = EnumSet.of(STRING, CHAR, PARAGRAPH);
    public static final Set<ClosureType> LEAFS = EnumSet.of(COMMENT, STRING, CHAR, PARAGRAPH);

    private static final List<ClosureType> CHECK_FIRST = List.of(PARAGRAPH);

    public static List<ClosureType> prioritySort(Set<ClosureType> types) {
        return types.stream()
            .sorted(Comparator.comparingInt(o -> {
                int index = CHECK_FIRST.indexOf(o);
                return index == -1 ? CHECK_FIRST.size() : index;
            }))
            .toList();
    }
}
