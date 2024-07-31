package io.papermc.typewriter.parser;

public class Lexer {

    // todo implement for unicode escape (and maybe closure?), these escapes are handled before any bytecode are emitted and affect the whole file

    public static boolean isWhitespace(int codePoint) {
        if (!Character.isBmpCodePoint(codePoint)) {
            return false; // just in case unicode is extended
        }

        return codePoint == ' ' ||
            codePoint == '\t' ||
            codePoint == '\f' ||
            codePoint == '\n' ||
            codePoint == '\r';
    }
}
