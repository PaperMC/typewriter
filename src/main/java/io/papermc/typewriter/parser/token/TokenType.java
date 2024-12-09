package io.papermc.typewriter.parser.token;

public enum TokenType {
    // reserved keywords (contextual keywords shouldn't be there, they are valid identifier for any var)
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
    LT("<"),
    GT(">"),
    DOT("."),
    SECO(";"),
    CO(","),
    // others
    STAR("*"),
    AT_SIGN("@"),
    COMMENT(null),
    SINGLE_COMMENT(null),
    JAVADOC(null),
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
