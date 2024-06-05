package io.papermc.typewriter.parser.closure;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public class AbstractClosure implements Closure {

    private final ClosureType type;
    private @Nullable Closure parent;

    protected AbstractClosure(ClosureType type) {
        this.type = type;
    }

    @Override
    public ClosureType getType() {
        return this.type;
    }

    @Override
    public @Nullable Closure parent() {
        return this.parent;
    }

    @Override
    public String toString() {
        return "Closure[" + this.type.name() + "](start=" + this.type.start + ", end=" + this.type.end + ")";
    }

    public void setParent(@Nullable Closure parent) {
        if (this.parent != null && parent != null) {
            throw new IllegalStateException("Cannot set this parent closure since it is already in a closure: " + this.parent);
        }
        this.parent = parent;
    }
}
