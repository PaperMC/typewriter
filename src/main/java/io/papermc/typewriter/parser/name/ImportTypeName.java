package io.papermc.typewriter.parser.name;

import io.papermc.typewriter.parser.ParserException;
import io.papermc.typewriter.parser.StringReader;

import java.util.function.Predicate;

public class ImportTypeName extends ProtoTypeName {

    public static final char IMPORT_ON_DEMAND_IDENTIFIER = '*';

    private final boolean isStatic;
    private String staticMemberName;
    private boolean global;

    private boolean locked;

    public ImportTypeName(Predicate<StringReader> cleaner, boolean isStatic) {
        super(cleaner);
        this.isStatic = isStatic;
    }

    public boolean isStatic() {
        return this.isStatic;
    }

    public String getStaticMemberName() {
        return this.staticMemberName;
    }

    public boolean isGlobal() {
        return this.global;
    }

    @Override
    protected void validateIdentifier(String identifier) {
        super.validateIdentifier(identifier);
        if (this.global) {
            this.name.delete(this.name.length() - 2, this.name.length()); // pop ".*"
        } else if (this.isStatic) {
            this.staticMemberName = identifier; // take a record of the last id
        }
    }

    @Override
    protected boolean isValid(int codePoint) {
        if (this.checkStartId && codePoint == IMPORT_ON_DEMAND_IDENTIFIER) {
            this.global = true;
            return true;
        }

        return super.isValid(codePoint);
    }

    @Override
    protected void append(char... chars) {
        if (this.locked) {
            throw new ParserException("Invalid java source, found a '%c' char in the middle of import type name".formatted(IMPORT_ON_DEMAND_IDENTIFIER), this.previousLine);
        } else if (this.global) {
            this.locked = true;
        }

        super.append(chars);
    }
}
