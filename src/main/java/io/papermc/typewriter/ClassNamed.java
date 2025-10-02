package io.papermc.typewriter;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.util.ClassHelper;
import io.papermc.typewriter.util.ClassResolver;
import javax.lang.model.SourceVersion;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.Contract;

import java.util.Objects;

@DefaultQualifier(NonNull.class)
public record ClassNamed(String packageName, String simpleName, String dottedNestedName, @Nullable Class<?> reference) {

    private ClassNamed(Class<?> reference) {
        this(reference.getPackageName(), reference.getSimpleName(), ClassHelper.retrieveFullNestedName(reference), reference);
    }

    public ClassNamed {
        Preconditions.checkArgument(packageName.isEmpty() || SourceVersion.isName(packageName), "Package name '%s' contains syntax errors", packageName);
        Preconditions.checkArgument(SourceVersion.isName(dottedNestedName), "Class name '%s' contains syntax errors", dottedNestedName);
        if (reference != null) {
            Preconditions.checkArgument(!reference.isPrimitive(), "Invalid class, primitive types and 'void' type are not allowed");
            Preconditions.checkArgument(!reference.isAnonymousClass() && !reference.isHidden() && !reference.isSynthetic() && !reference.isArray(), "Invalid class, only single named class are allowed");
        }
    }

    /**
     * Creates a class named object.
     *
     * @param packageName the package name or an empty string for the default package
     * @param name        the class name
     * @return the new object
     * @apiNote nested classes are delimited by '$' character and
     * class name using such character would be interpreted as is too.
     * To support those class names use {@link #of(String, String, String...)}
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static ClassNamed of(String packageName, String name) {
        int nestedIndex = name.lastIndexOf('$');
        final String simpleName;
        final String nestedName;
        if (nestedIndex != -1) {
            simpleName = name.substring(nestedIndex + 1);
            nestedName = name.replace('$', '.');
        } else {
            simpleName = name;
            nestedName = name;
        }
        return new ClassNamed(packageName, simpleName, nestedName, null);
    }

    /**
     * Creates a class named object.
     *
     * @param packageName the package name or an empty string for the default package
     * @param name        the top level class name
     * @param nestedNames the nested class names
     * @return the new object
     */
    @Contract(value = "_, _, _ -> new", pure = true)
    public static ClassNamed of(String packageName, String name, String... nestedNames) {
        final String simpleName;
        final String nestedName;
        if (nestedNames.length > 0) {
            simpleName = nestedNames[nestedNames.length - 1];
            nestedName = name + '.' + String.join(".", nestedNames);
        } else {
            simpleName = name;
            nestedName = name;
        }
        return new ClassNamed(packageName, simpleName, nestedName, null);
    }

    private static final ClassValue<ClassNamed> CACHE = new ClassValue<>() {
        @Override
        protected ClassNamed computeValue(Class<?> klass) {
            return new ClassNamed(klass);
        }
    };

    /**
     * Creates a class named object.
     *
     * @param reference the reference class
     * @return the new object
     */
    @Contract(value = "_ -> new", pure = true)
    public static ClassNamed of(Class<?> reference) {
        return CACHE.get(reference);
    }

    public ClassNamed resolve(ClassResolver resolver) {
        if (this.reference != null) {
            return this;
        }

        return resolver.find(this.binaryName())
            .map(ClassNamed::of)
            .orElseThrow(() -> new IllegalArgumentException("Cannot resolve class " + this));
    }

    public ClassNamed topLevel() {
        if (this.reference != null) {
            Class<?> topLevelClass = ClassHelper.getTopLevelClass(this.reference);
            if (topLevelClass == this.reference) {
                return this;
            }
            return ClassNamed.of(topLevelClass);
        }

        int dotIndex = this.dottedNestedName.indexOf('.');
        if (dotIndex != -1) {
            String name = this.dottedNestedName.substring(0, dotIndex);
            return new ClassNamed(this.packageName, name, name, null);
        }
        return this;
    }

    public boolean isTopLevel() {
        return this.dottedNestedName.equals(this.simpleName);
    }

    public @Nullable ClassNamed enclosing() {
        if (this.reference != null) {
            Class<?> parentClass = this.reference.getEnclosingClass();
            if (parentClass == null) {
                return null;
            }
            return ClassNamed.of(parentClass);
        }

        int dotIndex = this.dottedNestedName.lastIndexOf('.');
        if (dotIndex != -1) {
            String name = this.dottedNestedName.substring(0, dotIndex);

            int lastDotIndex = name.lastIndexOf('.');
            final String simpleName;
            if (lastDotIndex != -1) {
                simpleName = name.substring(lastDotIndex + 1);
            } else {
                simpleName = name; // top level
            }
            return new ClassNamed(this.packageName, simpleName, name, null);
        }
        return null;
    }

    public ClassNamed nested(String name) {
        if (this.reference != null) {
            try {
                Class<?> innerReference = Class.forName(this.reference.getName() + '$' + name);
                return ClassNamed.of(innerReference);
            } catch (ClassNotFoundException ignored) {
            }
        }

        return new ClassNamed(this.packageName, name, this.dottedNestedName + '.' + name, null);
    }

    public String relativize(ClassNamed otherType) {
        Preconditions.checkArgument(otherType.topLevel().equals(this.topLevel()), "Target class '%s' must be a nested class of %s", otherType.canonicalName(), this.canonicalName());
        int otherSize = otherType.dottedNestedName().length();
        int size = this.dottedNestedName().length();

        int maxOffset = Math.min(otherSize, size);
        int startOffset;
        for (startOffset = 0; startOffset < maxOffset; startOffset++) {
            if (this.dottedNestedName().charAt(startOffset) != otherType.dottedNestedName().charAt(startOffset)) {
                break;
            }
        }

        if (startOffset == otherSize) {
            // parent reference in nested class is always the shortest qualified name
            return otherType.simpleName();
        }

        if (otherType.dottedNestedName().charAt(startOffset) == '.') {
            startOffset++;
        }

        return otherType.dottedNestedName().substring(startOffset);
    }

    public String binaryName() {
        if (this.reference != null) {
            return this.reference.getName();
        }

        if (this.packageName.isEmpty()) {
            return this.dottedNestedName.replace('.', '$');
        }

        return this.packageName + '.' + this.dottedNestedName.replace('.', '$');
    }

    public String canonicalName() {
        if (this.reference != null) {
            return this.reference.getCanonicalName();
        }

        if (this.packageName.isEmpty()) {
            return this.dottedNestedName;
        }

        return this.packageName + '.' + this.dottedNestedName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.packageName, this.dottedNestedName);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }

        ClassNamed other = (ClassNamed) o;
        if (this.reference != null && other.reference != null) {
            return this.reference == other.reference;
        }
        return this.packageName.equals(other.packageName) &&
            this.dottedNestedName.equals(other.dottedNestedName);
    }
}
