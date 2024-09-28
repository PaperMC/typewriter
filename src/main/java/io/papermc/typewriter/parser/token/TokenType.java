package io.papermc.typewriter.parser.token;

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
    // punctuation marks
    LPAREN("("),
    RPAREN(")"),
    LSCOPE("{"),
    RSCOPE("}"),
    LBRACKET("["),
    RBRACKET("]"),
    DOT("."),
    SECO(";"),
    CO(","),
    // others
    STAR("*"),
    AT_SIGN("@"),
    COMMENT(null),
    SINGLE_COMMENT(null),
    JAVADOC(null),
    @ApiStatus.Experimental
    MARKDOWN_JAVADOC(null),
    IDENTIFIER(null),
    EOI(null);

    public final String name;
    TokenType(String name) {
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
