package io.papermc.typewriter.context;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.parser.token.TokenType;
import javax.lang.model.SourceVersion;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

import static io.papermc.typewriter.parser.name.ProtoQualifiedName.IDENTIFIER_SEPARATOR;

public interface ImportName extends Comparable<ImportName> {

    String IMPORT_ON_DEMAND_MARKER = TokenType.STAR.value;

    static String asGlobal(String name) {
        return dotJoin(name, IMPORT_ON_DEMAND_MARKER);
    }

    static String dotJoin(String prefix, String suffix) {
        return prefix.concat(String.valueOf(IDENTIFIER_SEPARATOR)).concat(suffix);
    }

    /**
     * Gets the full import name including its package name and type/member name.
     * It's includes the possible '*' in case of import on demand type
     *
     * @return the full name
     */
    String name();

    boolean newlyAdded();

    ImportCategory<?> category();

    interface Identified extends ImportName {

        /**
         * Gets the first identifier used to reference that import.
         * Only relevant for single imports.
         *
         * @return the first identifier
         */
        String id();

        boolean isGlobal();
    }

    @Override
    default int compareTo(ImportName other) {
        return this.name().compareTo(other.name());
    }

    record Type(String name, boolean isGlobal, boolean newlyAdded) implements Identified {

        public Type(ClassNamed className) {
            this(className.canonicalName(), false, true);
        }

        static Type fromQualifiedName(String qualifiedName) {
            Preconditions.checkArgument(SourceVersion.isName(qualifiedName), "Invalid import name '%s'", qualifiedName);
            boolean isGlobal = qualifiedName.endsWith(IMPORT_ON_DEMAND_MARKER);
            return new Type(qualifiedName, isGlobal, true);
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
            return this.name.equals(other.name) && this.isGlobal == other.isGlobal;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name, this.isGlobal);
        }
    }

    record Static(String name, String memberName, boolean isGlobal, boolean newlyAdded) implements Identified {

        static Static fromQualifiedMemberName(String qualifiedMemberName) {
            Preconditions.checkArgument(SourceVersion.isName(qualifiedMemberName), "Invalid static import name '%s'", qualifiedMemberName);
            boolean isGlobal = qualifiedMemberName.endsWith(IMPORT_ON_DEMAND_MARKER);
            final String memberName;
            if (isGlobal) {
                memberName = IMPORT_ON_DEMAND_MARKER;
            } else {
                memberName = qualifiedMemberName.substring(qualifiedMemberName.lastIndexOf(IDENTIFIER_SEPARATOR) + 1);
            }
            return new Static(qualifiedMemberName, memberName, isGlobal, true);
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

            String parentClasses = this.name.substring(packageName.length() + 1, this.name.length() - (this.isGlobal ? 1 : this.memberName.length())); // pop * at the end
            return memberName.substring(parentClasses.length());
        }

        public boolean isMemberImported(String packageName, String memberName) {
            if (this.isGlobal) {
                int dotIndex = memberName.lastIndexOf(IDENTIFIER_SEPARATOR);
                if (dotIndex == -1) {
                    return asGlobal(packageName).equals(this.name);
                }
                return asGlobal(dotJoin(packageName, memberName.substring(0, dotIndex))).equals(this.name);
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
            return this.name.equals(other.name) && this.isGlobal == other.isGlobal;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name, this.isGlobal);
        }
    }

    @ApiStatus.Experimental
    record Module(String name, boolean newlyAdded) implements ImportName {

        static Module fromQualifiedName(String qualifiedName) {
            Preconditions.checkArgument(SourceVersion.isName(qualifiedName), "Invalid module name '%s'", qualifiedName);
            return new Module(qualifiedName, true);
        }

        @Override
        public ImportCategory<Module> category() {
            return ImportCategory.MODULE;
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

            Module other = (Module) obj;
            return this.name.equals(other.name);
        }

        @Override
        public int hashCode() {
            return this.name.hashCode();
        }
    }
}
