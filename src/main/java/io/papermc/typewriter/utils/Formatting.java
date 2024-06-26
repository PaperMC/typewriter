package io.papermc.typewriter.utils;

import io.papermc.typewriter.parser.ProtoTypeName;
import javax.lang.model.SourceVersion;

import java.lang.reflect.Member;
import java.util.regex.Pattern;

public final class Formatting {

    public static int countOccurrences(String value, char match) {
        int count = 0;
        for (int i = 0, len = value.length(); i < len; i++) {
            if (value.charAt(i) == match) {
                count++;
            }
        }
        return count;
    }

    public static String quoted(String value) {
        return "\"" + value + "\"";
    }

    public static String asCode(Object object) {
        return switch (object) {
            case null -> "null";
            case Enum<?> enumValue -> enumValue.getClass().getSimpleName() + "." + enumValue.name();
            case Member member -> member.getName();
            case Long l -> l.toString() + "L";
            case Float f -> f.toString() + "F";
            default -> object.toString();
        };
    }

    public static final Pattern NAME_SEPARATOR = Pattern.compile(String.valueOf(ProtoTypeName.IDENTIFIER_SEPARATOR), Pattern.LITERAL);

    // check only syntax error and keywords but not if each part are valid identifier
    public static boolean isValidName(String name) {
        for (String part : NAME_SEPARATOR.split(name)) {
            if (part.isEmpty() || SourceVersion.isKeyword(part)) {
                return false;
            }
        }
        return true;
    }

    private Formatting() {
    }
}
