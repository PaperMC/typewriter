package io.papermc.typewriter.parser.closure;

import java.util.EnumSet;
import java.util.Set;

public enum ClosureType {
    PARENTHESIS("(", ")"),
    CURLY_BRACKET("{", "}"),
    BRACKET("[", "]"),
    COMMENT("/*", "*/"),
    PARAGRAPH("\"\"\"", "\"\"\""), // order matter here (paragraph > string)
    STRING("\"", "\""),
    CHAR("'", "'");

    private static final Set<ClosureType> ALLOW_ESCAPE = EnumSet.of(STRING, CHAR, PARAGRAPH);
    public static final Set<ClosureType> LEAFS = EnumSet.of(COMMENT, STRING, CHAR, PARAGRAPH);

    public final String start;
    public final String end;

    ClosureType(String start, String end) {
        this.start = start;
        this.end = end;
    }

    public boolean escapableByPreviousChar() {
        return ALLOW_ESCAPE.contains(this) && this.end.length() == 1 && this.start.equals(this.end);
    }
}
