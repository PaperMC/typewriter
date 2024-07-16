package name;

import name.one.OneDepthClass.NonStaticClass;
import name.one.OneDepthClass;
import static name.one.OneDepthClass.StaticClass;

public class RemoteInnerClassImportType {
    {
        var a = NonStaticClass.class;
        var b = NonStaticClass.Inner.class;
        var c = OneDepthClass.NonStaticClass2.class;
        var d = StaticClass.class;
    }
}
