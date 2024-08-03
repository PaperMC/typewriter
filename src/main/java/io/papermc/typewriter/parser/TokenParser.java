package io.papermc.typewriter.parser;

import io.papermc.typewriter.context.ImportCollector;
import io.papermc.typewriter.context.ImportTypeCollector;
import io.papermc.typewriter.parser.exception.ParserException;
import io.papermc.typewriter.parser.name.ImportTypeName;
import io.papermc.typewriter.parser.token.CharSequenceToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;
import org.checkerframework.checker.nullness.qual.Nullable;
import javax.lang.model.SourceVersion;

import java.util.EnumSet;
import java.util.Set;

public class TokenParser {

    private final Lexer lexer;

    public TokenParser(Lexer lexer) {
        this.lexer = lexer;
    }

    private static final Set<TokenType> FORMAT_TOKENS = EnumSet.of(
        TokenType.COMMENT,
        TokenType.SINGLE_COMMENT,
        TokenType.JAVADOC,
        TokenType.MARKDOWN_JAVADOC
    );

    private ImportTypeName readImport() {
        ImportTypeName typeName = new ImportTypeName();
        boolean canReadId = true;
        while (this.lexer.canRead()) {
            Token token = this.lexer.readToken();

            if (token.type() == TokenType.EOI ||
                token.type() == TokenType.SECO) {
                break;
            }
            if (FORMAT_TOKENS.contains(token.type())) {
                continue; // ignore
            }

            if (typeName.isGlobal()) {
                throw new ParserException("Trailing token found after import on demand marker ('*')", token);
            }

            if (token.type() == TokenType.STATIC) {
                if (typeName.isStatic()) {
                    throw new ParserException("Duplicate static keyword found inside import statement", token);
                }
                typeName.setStatic();
                continue;
            }

            if (token.type() == TokenType.IDENTIFIER) {
                CharSequenceToken idToken = (CharSequenceToken) token;
                String identifier = idToken.value();
                if (!canReadId || identifier.isEmpty()) {
                    throw new ParserException("Invalid java source, type name contains a syntax error", token);
                }

                if (SourceVersion.isKeyword(identifier)) {
                    throw new ParserException("Invalid java source, type name contains a reserved keyword (" + identifier + ")", token);
                }

                typeName.append(identifier);
                canReadId = false;
            } else if (token.type() == TokenType.DOT) {
                if (canReadId || typeName.isEmpty()) {
                    throw new ParserException("Invalid java source, type name contains a syntax error", token);
                }
                typeName.appendSeparator();
                canReadId = true;
            } else if (!typeName.isEmpty() && token.type() == TokenType.STAR) {
                if (!canReadId || typeName.isEmpty()) {
                    throw new ParserException("Invalid java source, type name contains a syntax error", token);
                }
                typeName.popSeparator();
                typeName.setGlobal();
                canReadId = false;
            } else {
                throw new ParserException("Illegal token found after import keyword", token);
            }
        }

        if (typeName.isEmpty()) {
            throw new IllegalStateException("Invalid java source, import type name is empty");
        }
        return typeName;
    }

    private void skipAnnotation() {
        int parenDepth = 0;
        boolean firstId = true;
        while (this.lexer.canRead()) {
            Token token = this.lexer.readToken();

            if (token.type() == TokenType.EOI) {
                break;
            }

            if (FORMAT_TOKENS.contains(token.type())) {
                continue; // ignore
            }

            if (firstId && token.type() == TokenType.INTERFACE) {
                break;
            }

            if (token.type() == TokenType.IDENTIFIER) {
                firstId = false;
                continue;
            }

            if (!firstId && token.type() == TokenType.DOT) {
                continue;
            }

            if (token.type() == TokenType.LPAREN) {
                parenDepth++;
            } else if (token.type() == TokenType.RPAREN) {
                parenDepth--;
            }

            if (parenDepth == 0) {
                break;
            }

            if (parenDepth < 0) {
                throw new ParserException("Unbalanced parenthesis inside annotation value", token);
            }
        }
    }

    public @Nullable Token collectImports(ImportCollector collector) {
        while (this.lexer.canRead()) {
            Token token = this.lexer.readToken();

            if (token.type() == TokenType.EOI) {
                break;
            }

            if (token.type() == TokenType.LSCOPE) {
                return token; // reach top level body, return token just for the test
            }

            if (token.type() == TokenType.IMPORT) {
                ((ImportTypeCollector) collector).addProtoImport(this.readImport());
            } else if (token.type() == TokenType.AT_SIGN) {
                this.skipAnnotation();
            }
        }
        return null;
    }
}
