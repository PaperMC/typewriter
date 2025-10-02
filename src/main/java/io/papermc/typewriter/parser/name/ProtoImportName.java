package io.papermc.typewriter.parser.name;

import io.papermc.typewriter.context.ImportCategory;

public class ProtoImportName extends ProtoQualifiedName {

    private ImportCategory<?> category = ImportCategory.TYPE;
    private boolean isWilddcard;
    private String staticMemberName;

    public ImportCategory<?> getCategory() {
        return this.category;
    }

    public boolean isWilddcard() {
        return this.isWilddcard;
    }

    public void asCategory(ImportCategory<?> category) {
        this.category = category;
    }

    public void asWildcard() {
        this.isWilddcard = true;
    }

    public String getStaticMemberName() {
        return this.staticMemberName;
    }

    @Override
    public void append(String identifier) {
        super.append(identifier);
        if (this.category == ImportCategory.STATIC) {
            this.staticMemberName = identifier;
        }
    }
}
