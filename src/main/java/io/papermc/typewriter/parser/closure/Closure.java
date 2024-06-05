package io.papermc.typewriter.parser.closure;

import io.papermc.typewriter.parser.ParserException;
import io.papermc.typewriter.parser.StringReader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import java.util.function.Supplier;

@DefaultQualifier(NonNull.class)
public interface Closure {

    ClosureType getType();

    @Nullable Closure parent();

    default boolean hasUpperClosure(Closure closure) {
        @Nullable Closure parent = this;
        do {
            if (parent == closure) {
                return true;
            }
            parent = parent.parent();
        } while (parent != null);
        return false;
    }

    default void onStart(StringReader line) {

    }

    default void onEnd(StringReader line) {

    }

    static Closure create(ClosureType type) {
        class SpecialClosure {
            private static final Supplier<Closure> PARAGRAPH = () -> new AbstractClosure(ClosureType.PARAGRAPH) {

                @Override
                public void onStart(StringReader line) {
                    if (line.canRead()) {
                        throw new ParserException("Paragraph closure start must be followed by a newline", line);
                    }
                }
            };
        }

        if (type == ClosureType.PARAGRAPH) {
            return SpecialClosure.PARAGRAPH.get();
        }
        return new AbstractClosure(type);
    }
}
