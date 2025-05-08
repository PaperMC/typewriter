package io.papermc.typewriter.parser.token;

public enum TokenType {
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
}
