package io.papermc.typewriter.parser;

import io.papermc.typewriter.SourceFile;
import io.papermc.typewriter.context.ImportCollector;
import io.papermc.typewriter.context.ImportNameCollector;
import io.papermc.typewriter.parser.name.ProtoImportName;
import io.papermc.typewriter.parser.sequence.SequenceTokens;
import io.papermc.typewriter.parser.sequence.TokenTaskBuilder;
import io.papermc.typewriter.parser.sequence.hook.HookType;
import io.papermc.typewriter.parser.token.pos.TokenCapture;
import io.papermc.typewriter.parser.token.pos.TokenRecorder;
import io.papermc.typewriter.parser.token.PrintableToken;
import io.papermc.typewriter.parser.token.TokenType;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

public final class ImportParser {

    private static final Set<TokenType> FORMAT_TOKENS = EnumSet.of(
        TokenType.COMMENT,
        TokenType.SINGLE_COMMENT,
        TokenType.JAVADOC,
        TokenType.MARKDOWN_JAVADOC
    );

    public static void collectImports(Tokenizer tokenizer, ImportCollector collector, SourceFile source) {
        SequenceTokens.wrap(tokenizer, FORMAT_TOKENS)
            .skip(TokenType.PACKAGE, action -> { // package <qualified name>;
                action.skipQualifiedName().skip(TokenType.SECO);
            }, TokenTaskBuilder::asOptional) // for default package
            .skip(TokenType.IMPORT, action -> {
                ProtoImportName protoName = new ProtoImportName();
                action
                    .map(TokenType.STATIC, stat -> protoName.asStatic(), TokenTaskBuilder::asOptional)
                    .mapQualifiedName(
                        name -> protoName.append(name.value()),
                        dot -> protoName.appendSeparator(),
                        partialAction -> partialAction
                            .map(TokenType.STAR, star -> {
                                protoName.append(TokenType.STAR.value);
                                protoName.asGlobal();
                            })
                    )
                    .map(TokenType.SECO, $ -> {
                        ((ImportNameCollector) collector).addProtoImport(protoName);
                    });
                },
                params -> params.asOptional().asRepeatable()
            )
            .executeOrThrow((failedTask, token) -> failedTask.createFailure("Wrong token found while collecting imports", token).withAdditionalContext(source));
    }

    public static TokenCapture trackImportPosition(Tokenizer tokenizer) {
        TokenRecorder.Default<PrintableToken> tokenPos = TokenRecorder.BETWEEN_TOKEN.record();
        SequenceTokens.wrap(tokenizer, FORMAT_TOKENS)
            .skip(TokenType.PACKAGE, action -> { // package <qualified name>;
                action.skipQualifiedName().skip(TokenType.SECO);
            }, TokenTaskBuilder::asOptional) // for default package
            .skip(TokenType.IMPORT, action -> {
                action
                    .skip(TokenType.STATIC, TokenTaskBuilder::asOptional)
                    .skipQualifiedName((Consumer<SequenceTokens>) (partialAction) -> partialAction.skip(TokenType.STAR))
                    .map(TokenType.SECO, tokenPos::end);
                },
                params -> params.asOptional().asRepeatable().hooks(manager -> {
                    manager.bind(HookType.FIRST, hook -> hook.pre(tokenPos::begin));
                })
            )
            .executeOrThrow((failedTask, token) -> failedTask.createFailure("Wrong token found while tracking import section position", token));
        return tokenPos.fetch();
    }

    private ImportParser() {
    }
}
