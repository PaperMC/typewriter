package io.papermc.typewriter.util;

public final class ClassHelper {

    public static Class<?> getTopLevelClass(Class<?> clazz) {
        Class<?> topLevelClass = clazz;
        Class<?> upperClass = clazz;
        while (true) {
            upperClass = upperClass.getEnclosingClass();
            if (upperClass == null) {
                break;
            }
            topLevelClass = upperClass;
        }
        return topLevelClass;
    }

    public static String retrieveFullNestedName(Class<?> clazz) {
        String fqn = clazz.getCanonicalName();
        String packageName = clazz.getPackageName();
        if (packageName.isEmpty()) {
            return fqn;
        }
        return fqn.substring(packageName.length() + 1);
    }

    private ClassHelper() {
    }
}
