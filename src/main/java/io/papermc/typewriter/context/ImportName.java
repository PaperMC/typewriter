package io.papermc.typewriter.context;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.parser.token.TokenType;
import javax.lang.model.SourceVersion;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.Objects;

import static io.papermc.typewriter.parser.name.ProtoQualifiedName.IDENTIFIER_SEPARATOR;

@DefaultQualifier(NonNull.class)
public sealed interface ImportName extends Comparable<ImportName> permits ImportName.Identified {

    String IMPORT_ON_DEMAND_MARKER = TokenType.STAR.value;

    static String asWildcard(String name) {
        return dotJoin(name, IMPORT_ON_DEMAND_MARKER);
    }

    static String dotJoin(String prefix, String suffix) {
        return prefix.concat(String.valueOf(IDENTIFIER_SEPARATOR)).concat(suffix);
    }

    /**
     * Gets the full import name including its package name and type/member name.
     * This includes the possible '*' in case of import on demand type
     *
     * @return the full name
     */
    String name();

    boolean newlyAdded();

    ImportCategory<?> category();

    sealed interface Identified extends ImportName permits ImportName.Type, ImportName.Static {

        /**
         * Gets the first identifier used to reference that import.
         * Only relevant for single imports.
         *
         * @return the first identifier
         */
        String id();

        boolean isWildcard();
    }

    @Override
    default int compareTo(ImportName other) {
        return this.name().compareTo(other.name());
    }

    record Type(String name, boolean isWildcard, boolean newlyAdded) implements Identified {

        public Type(ClassNamed className) {
            this(className.canonicalName(), false, true);
        }

        static Type fromQualifiedName(String qualifiedName) {
            Preconditions.checkArgument(SourceVersion.isName(qualifiedName), "Invalid import name '%s'", qualifiedName);
            boolean isWildcard = qualifiedName.endsWith(IMPORT_ON_DEMAND_MARKER);
            return new Type(qualifiedName, isWildcard, true);
        }

        @Override
        public String id() {
            int dotIndex = this.name.lastIndexOf(IDENTIFIER_SEPARATOR);
            return dotIndex == -1 ? this.name : this.name.substring(dotIndex + 1);
        }

        @Override
        public ImportCategory<Type> category() {
            return ImportCategory.TYPE;
        }

        // exclude newlyAdded
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }

            Type other = (Type) obj;
            return this.name.equals(other.name) && this.isWildcard == other.isWildcard;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name, this.isWildcard);
        }
    }

    record Static(String name, String memberName, boolean isWildcard, boolean newlyAdded) implements Identified {

        static Static fromQualifiedMemberName(String qualifiedMemberName) {
            Preconditions.checkArgument(SourceVersion.isName(qualifiedMemberName), "Invalid static import name '%s'", qualifiedMemberName);
            boolean isWildcard = qualifiedMemberName.endsWith(IMPORT_ON_DEMAND_MARKER);
            final String memberName;
            if (isWildcard) {
                memberName = IMPORT_ON_DEMAND_MARKER;
            } else {
                memberName = qualifiedMemberName.substring(qualifiedMemberName.lastIndexOf(IDENTIFIER_SEPARATOR) + 1);
            }
            return new Static(qualifiedMemberName, memberName, isWildcard, true);
        }

        @Override
        public String id() {
            return this.memberName;
        }

        public String resolveMemberName(String packageName, String memberName) {
            int dotIndex = memberName.indexOf(IDENTIFIER_SEPARATOR);
            if (dotIndex == -1) {
                return memberName;
            }

            String ownerClasses = this.name.substring(packageName.length() + 1, this.name.length() - this.memberName.length());
            return memberName.substring(ownerClasses.length());
        }

        public boolean isMemberImported(String packageName, String memberName) {
            if (this.isWildcard) {
                int dotIndex = memberName.lastIndexOf(IDENTIFIER_SEPARATOR);
                if (dotIndex == -1) {
                    return asWildcard(packageName).equals(this.name);
                }
                return asWildcard(dotJoin(packageName, memberName.substring(0, dotIndex))).equals(this.name);
            }

            return dotJoin(packageName, memberName).equals(this.name);
        }

        @Override
        public ImportCategory<Static> category() {
            return ImportCategory.STATIC;
        }

        // exclude newlyAdded
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }

            Static other = (Static) obj;
            return this.name.equals(other.name) && this.isWildcard == other.isWildcard;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name, this.isWildcard);
        }
    }
}
