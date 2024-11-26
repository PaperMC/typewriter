package name;

import name.one.OneDepthClass.NonStaticClass;
import name.one.OneDepthClass;

public class RemoteNestedClassImportType {
    {
        var a = NonStaticClass.class;
        var b = NonStaticClass.Inner.class;
        var c = OneDepthClass.NonStaticClass2.class;
    }
}
