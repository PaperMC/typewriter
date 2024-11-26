package io.papermc.typewriter.parser.token;

import org.jetbrains.annotations.ApiStatus;

public enum TokenType {
    // keywords
    PACKAGE("package"),
    IMPORT("import"),
    STATIC("static"),
    PUBLIC("public"),
    FINAL("final"),
    INTERFACE("interface"),
    // literals
    CHAR(null),
    STRING(null),
    PARAGRAPH(null),
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

    public final String value;
    TokenType(String value) {
        this.value = value;
    }

    public static TokenType fromValue(String value, TokenType def) {
        for (TokenType type : TokenType.values()) {
            if (type.value != null && type.value.equals(value)) {
                return type;
            }
        }
        return def;
    }
}
