package io.papermc.typewriter.context;

import io.papermc.typewriter.ClassNamed;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.ApiStatus;

public interface ImportCollector {

    void setAccessSource(@Nullable ClassNamed accessSource);

    void addImport(String typeName);

    void addStaticImport(String fullName);

    boolean canImportSafely(ClassNamed type);

    String getStaticMemberShortName(String fullName);

    default String getShortName(Class<?> type) {
        return this.getShortName(new ClassNamed(type));
    }

    default String getShortName(ClassNamed type) {
        return this.getShortName(type, true);
    }

    default String getShortName(Class<?> type, boolean autoImport) {
        return this.getShortName(new ClassNamed(type), autoImport);
    }

    String getShortName(ClassNamed type, boolean autoImport);

    @ApiStatus.Experimental
    default String getShortestName(Class<?> type) {
        return this.getShortestName(new ClassNamed(type));
    }

    @ApiStatus.Experimental
    String getShortestName(ClassNamed type);
}
