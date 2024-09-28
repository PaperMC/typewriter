package name;

import static name.one.OneDepthClass.*;

public class RemoteGlobalStaticNestedClassImportType {
    {
        //var a = name.one.OneDepthClass.NonStaticClass.class;
        var b = StaticClass.class;
    }
}
