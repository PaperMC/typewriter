package name;

import static name.one.OneDepthClass.*;
import static name.one.OneDepthClass.StaticClass.*;

public class RemoteGlobalStaticNestedClassImportType {
    {
        //var a = name.one.OneDepthClass.NonStaticClass.class;
        var b = StaticClass.class;
        var c = Inner.class;
        var d = Inner.Inner2.class;
    }
}
