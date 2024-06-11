package io.papermc.typewriter;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.utils.Formatting;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.Contract;

@DefaultQualifier(NonNull.class)
public record IndentUnit(String content, int size, char character) implements CharSequence {

    @Contract(value = "_ -> new", pure = true)
    public static IndentUnit parseUnsafe(String content) {
        return new IndentUnit(content, content.length(), content.charAt(0));
    }

    /**
     * Creates an indent unit compatible with a source file.
     * <br>
     * The indent unit characters must follow these conditions:
     *
     * <ul>
     *   <li>all the characters in the string must use the same character repeated if needed.
     *   <li>the character used is either a space (0x20) or a tab (0x09).
     * </ul>
     *
     * @param content the indent unit content
     * @return the new object
     * @see io.papermc.typewriter.SourceFile
     */
    @Contract(value = "_ -> new", pure = true)
    public static IndentUnit parse(String content) {
        Preconditions.checkArgument(!content.isEmpty(), "Indent unit cannot be empty!");
        char c0 = content.charAt(0);
        int size = content.length();

        for (int i = 0; i < size; i++) {
            char c = content.charAt(i);
            Preconditions.checkArgument(c == ' ' || c == '\t', "Character '%s' is not an indentation character!", c);
            Preconditions.checkArgument(c0 == c, "Indent unit contains mixed character (%s != %s)", c, c0);
        }

        return new IndentUnit(content, size, c0);
    }

    @Override
    public int length() {
        return this.size;
    }

    @Override
    public char charAt(int index) {
        Preconditions.checkElementIndex(index, this.size);
        return this.character; // mixed character are not allowed
    }

    @Override
    public CharSequence subSequence(int beginIndex, int endIndex) {
        return this.content.subSequence(beginIndex, endIndex);
    }

    public String adjustContentFor(ClassNamed classNamed) {
        if (classNamed.knownClass() == null) {
            return this.content.repeat(Formatting.countOccurrences(classNamed.dottedNestedName(), '.') + 1);
        }

        Class<?> parent = classNamed.knownClass().getEnclosingClass();
        StringBuilder indentBuilder = new StringBuilder(this.content);
        while (parent != null) {
            indentBuilder.append(this.content);
            parent = parent.getEnclosingClass();
        }
        return indentBuilder.toString();
    }
}
