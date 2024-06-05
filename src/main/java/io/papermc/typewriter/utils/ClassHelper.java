package io.papermc.typewriter.utils;

public final class ClassHelper {

    public static Class<?> getRootClass(Class<?> clazz) {
        Class<?> rootClass = clazz;
        Class<?> upperClass = clazz;
        while (true) {
            upperClass = upperClass.getEnclosingClass();
            if (upperClass == null) {
                break;
            }
            rootClass = upperClass;
        }
        return rootClass;
    }

    public static String retrieveFullNestedName(Class<?> clazz) {
        String fqn = clazz.getCanonicalName();
        return fqn.substring(clazz.getPackageName().length() + 1);
    }

    private ClassHelper() {
    }
}
