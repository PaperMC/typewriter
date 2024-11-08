package io.papermc.typewriter.parser;

import io.papermc.typewriter.context.ImportCollector;
import io.papermc.typewriter.context.ImportTypeCollector;
import io.papermc.typewriter.parser.name.ProtoImportTypeName;
import io.papermc.typewriter.parser.token.TokenBlockPosition;
import io.papermc.typewriter.parser.token.TokenType;

import java.util.EnumSet;
import java.util.Set;

public final class ImportParser {

    private static final Set<TokenType> FORMAT_TOKENS = EnumSet.of(
        TokenType.COMMENT,
        TokenType.SINGLE_COMMENT,
        TokenType.JAVADOC,
        TokenType.MARKDOWN_JAVADOC
    );

    public static void collectImports(Lexer lexer, ImportCollector collector) {
        SequenceTokens.wrap(lexer, FORMAT_TOKENS)
            .skip(TokenType.PACKAGE, action -> { // package <qualified name>;
                action.skipQualifiedName().skip(TokenType.SECO);
            }, SequenceTokens.TokenTask::asOptional) // for default package
            .skip(TokenType.IMPORT, action -> {
                ProtoImportTypeName protoImportTypeName = new ProtoImportTypeName();
                action
                    .map(TokenType.STATIC, stat -> protoImportTypeName.asStatic(), SequenceTokens.TokenTask::asOptional)
                    .mapQualifiedName(
                        name -> protoImportTypeName.append(name.value()),
                        dot -> protoImportTypeName.appendSeparator(),
                        partialAction -> partialAction
                            .map(TokenType.STAR, star -> {
                                protoImportTypeName.popSeparator();
                                protoImportTypeName.asGlobal();
                            })
                    )
                    .map(TokenType.SECO, $ -> {
                        ((ImportTypeCollector) collector).addProtoImport(protoImportTypeName);
                    });
                },
                SequenceTokens.TokenTask::asRepeatable
            )
            .executeOrThrow((failedTask, token) -> failedTask.createFailure("Wrong token found while collecting imports", token));
    }

    public static TokenBlockPosition trackImportPosition(Lexer lexer) {
        TokenBlockPosition tokenPos = new TokenBlockPosition();
        SequenceTokens.wrap(lexer, FORMAT_TOKENS)
            .skip(TokenType.PACKAGE, action -> { // package <qualified name>;
                action.skipQualifiedName().skip(TokenType.SECO);
            }, SequenceTokens.TokenTask::asOptional) // for default package
            .skip(TokenType.IMPORT, action -> {
                action
                    .skip(TokenType.STATIC, SequenceTokens.TokenTask::asOptional)
                    .skipQualifiedName(partialAction -> partialAction.skip(TokenType.STAR))
                    .map(TokenType.SECO, tokenPos::end);
                },
                params -> params.asRepeatable(tokenPos::begin)
            )
            .executeOrThrow((failedTask, token) -> failedTask.createFailure("Wrong token found tracking import section position", token));
        return tokenPos;
    }

    private ImportParser() {
    }
}
