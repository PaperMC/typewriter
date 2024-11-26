package name;

import name.one.OneDepthClass.*;
import name.one.OneDepthClass.StaticClass.*;

public class RemoteGlobalNestedClassImportType {
    {
        var a = NonStaticClass.class;
        var b = StaticClass.class;
        var c = Inner.class;
        var d = Inner.Inner2.class;
    }
}
