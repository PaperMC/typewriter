package io.papermc.typewriter.parser.name;


public class ProtoImportTypeName extends ProtoTypeName {

    private boolean isStatic;
    private boolean isGlobal;
    private String staticMemberName;

    public boolean isStatic() {
        return this.isStatic;
    }

    public boolean isGlobal() {
        return this.isGlobal;
    }

    public void asStatic() {
        this.isStatic = true;
    }

    public void asGlobal() {
        this.isGlobal = true;
    }

    public String getStaticMemberName() {
        return this.staticMemberName;
    }

    @Override
    public void append(String identifier) {
        super.append(identifier);
        if (!this.isGlobal && this.isStatic) {
            this.staticMemberName = identifier;
        }
    }
}
