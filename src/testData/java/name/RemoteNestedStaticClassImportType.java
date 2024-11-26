package name;

import static name.one.OneDepthClass.StaticClass;
import static name.one.OneDepthClass.StaticClass2.Inner;

public class RemoteNestedStaticClassImportType {
    {
        var a = StaticClass.class;
        var b = StaticClass.Inner.class;
        var c = StaticClass.Inner.Inner2.class;
        var d = Inner.class;
        var e = Inner.Inner2.class;
    }
}
