package name;

import name.SamePackageClass.Inner;
import name.one.*;
import name.one.OneDepthClass.NonStaticClass2;

public class PackageClassImportType {
    {
        var a = SamePackageClass.class;
        var b = Inner.class;
        var c = OneDepthClass.class;
        var d = OneDepthClass.NonStaticClass.class;
        var e = OneDepthClass.NonStaticClass.Inner.class;
        var f = NonStaticClass2.class;
        var g = OneDepthClass.StaticClass.class;
    }
}
