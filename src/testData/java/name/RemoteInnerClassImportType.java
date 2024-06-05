package name;

import name.one.OneDepthClass.NonStaticClass;
import static name.one.OneDepthClass.StaticClass;

public class RemoteInnerClassImportType {
    {
        var a = NonStaticClass.class;
        var b = StaticClass.class;
    }
}
