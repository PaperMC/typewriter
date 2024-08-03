package io.papermc.typewriter.parser.token;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.ApiStatus;

public enum TokenType {
    // keywords
    IMPORT("import"),
    STATIC("static"),
    PUBLIC("public"),
    FINAL("final"),
    INTERFACE("interface"),
    // literals
    CHAR("'"),
    STRING("\""),
    PARAGRAPH("\"\"\""),
    // others
    LPAREN("("),
    RPAREN(")"),
    LSCOPE("{"),
    RSCOPE("}"),
    LBRACKET("["),
    RBRACKET("]"),
    STAR("*"),
    AT_SIGN("@"),
    DOT("."),
    SECO(";"),
    CO(","),
    COMMENT(null),
    SINGLE_COMMENT(null),
    JAVADOC(null),
    @ApiStatus.Experimental
    MARKDOWN_JAVADOC(null),
    IDENTIFIER(null),
    EOI(null);

    public final @Nullable String name;
    TokenType(@Nullable String name) {
        this.name = name;
    }

    public static TokenType fromName(String name, TokenType def) {
        for (TokenType type : TokenType.values()) {
            if (type.name != null && type.name.equals(name)) {
                return type;
            }
        }
        return def;
    }
}
