package io.papermc.typewriter.parser;

import io.papermc.typewriter.SourceFile;
import io.papermc.typewriter.context.ImportCategory;
import io.papermc.typewriter.context.ImportCollector;
import io.papermc.typewriter.context.ImportNameCollector;
import io.papermc.typewriter.parser.name.ProtoImportName;
import io.papermc.typewriter.parser.sequence.SequenceTokens;
import io.papermc.typewriter.parser.sequence.TokenTaskBuilder;
import io.papermc.typewriter.parser.sequence.hook.HookType;
import io.papermc.typewriter.parser.token.CharSequenceToken;
import io.papermc.typewriter.parser.token.pos.TokenCapture;
import io.papermc.typewriter.parser.token.pos.TokenRecorder;
import io.papermc.typewriter.parser.token.PrintableToken;
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

    public static void collectImports(Tokenizer tokenizer, ImportCollector collector, boolean includeModule, SourceFile source) {
        SequenceTokens.wrap(tokenizer, FORMAT_TOKENS)
            .skip(TokenType.PACKAGE, action -> { // package <qualified name>;
                action.skipQualifiedName().skip(TokenType.SECO);
            }, TokenTaskBuilder::asOptional) // for default package
            .skip(TokenType.IMPORT, action -> {
                ProtoImportName protoName = new ProtoImportName();
                action
                    .map(TokenType.STATIC, $ -> protoName.asCategory(ImportCategory.STATIC))
                    .orMap((token, reader) -> {
                        if (!includeModule) {
                            return false;
                        }

                        if (token.type() == TokenType.IDENTIFIER && ((CharSequenceToken) token).value().equals(ImportCategory.MODULE.identity().orElseThrow())) {
                            PrintableToken nextToken = reader.next();
                            if (nextToken != null && nextToken.type() == TokenType.IDENTIFIER) {
                                return true;
                            } else {
                                // module is a contextual keyword meaning it can be used as a regular identifier elsewhere
                                // if the qualified name starts with "module" it should *not* be considered as a module import
                                // import module A; <-/-> import module.A;
                                reader.reset();
                            }
                        }
                        return false;
                    },token -> protoName.asCategory(ImportCategory.MODULE), TokenTaskBuilder::asOptional)
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
            .executeOrThrow(failedTask -> failedTask.createFailure("Wrong token found while collecting imports").withAdditionalContext(source));
    }

    public static TokenCapture trackImportPosition(Tokenizer tokenizer, boolean includeModule) {
        TokenRecorder.Default<PrintableToken> tokenPos = TokenRecorder.BETWEEN_TOKEN.record();
        SequenceTokens.wrap(tokenizer, FORMAT_TOKENS)
            .skip(TokenType.PACKAGE, action -> { // package <qualified name>;
                action.skipQualifiedName().skip(TokenType.SECO);
            }, TokenTaskBuilder::asOptional) // for default package
            .skip(TokenType.IMPORT, action -> {
                action
                    .skip(TokenType.STATIC)
                    .orSkip((token, reader) -> {
                        if (!includeModule) {
                            return false;
                        }

                        if (token.type() == TokenType.IDENTIFIER && ((CharSequenceToken) token).value().equals(ImportCategory.MODULE.identity().orElseThrow())) {
                            PrintableToken nextToken = reader.next();
                            if (nextToken != null && nextToken.type() == TokenType.IDENTIFIER) {
                                return true;
                            } else {
                                // module is a contextual keyword meaning it can be used as a regular identifier elsewhere
                                // if the qualified name starts with "module" it should *not* be considered as a module import
                                // import module A; <-/-> import module.A;
                                reader.reset();
                            }
                        }
                        return false;
                    }, TokenTaskBuilder::asOptional)
                    .skipQualifiedName((SequenceTokens partialAction) -> partialAction.skip(TokenType.STAR))
                    .map(TokenType.SECO, tokenPos::end);
                },
                params -> params.asOptional().asRepeatable().hooks(manager -> {
                    manager.bind(HookType.FIRST, hook -> hook.pre(tokenPos::begin));
                })
            )
            .executeOrThrow(failedTask -> failedTask.createFailure("Wrong token found while tracking import section position"));
        return tokenPos.fetch();
    }

    private ImportParser() {
    }
}
