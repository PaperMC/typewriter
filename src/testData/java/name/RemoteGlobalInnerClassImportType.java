package name;

import name.one.OneDepthClass.*;

public class RemoteGlobalInnerClassImportType {
    {
        var a = NonStaticClass.class;
        var b = StaticClass.class;
    }
}
