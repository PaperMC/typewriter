package name;

import name.one.*;

public class PackageClassImportType {
    {
        var a = SamePackageClass.class;
        var b = OneDepthClass.class;
        var c = OneDepthClass.NonStaticClass.class;
        var d = OneDepthClass.StaticClass.class;
    }
}
