package io.papermc.typewriter.parser.name;


public class ProtoImportTypeName extends ProtoTypeName {

    private boolean isStatic;
    private String staticMemberName;
    private boolean global;

    public boolean isStatic() {
        return this.isStatic;
    }

    public String getStaticMemberName() {
        return this.staticMemberName;
    }

    public boolean isGlobal() {
        return this.global;
    }

    public void setStatic() {
        this.isStatic = true;
    }

    public void setGlobal() {
        this.global = true;
    }

    @Override
    public void append(String identifier) {
        super.append(identifier);
        this.staticMemberName = identifier;
    }
}
