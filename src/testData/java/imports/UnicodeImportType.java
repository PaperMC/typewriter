package imports;

// 𗙤 gradle panic on special character so no test for them :/

import java.util.\u000A\u000DList; // line terminator
import imports.UnicodeImportType.ÉàAΩType;
import\uuuuu0020static\u000Cimports.\u0009UnicodeImportType\uuuuu0020.ÉàAΩType; // space
/* surrogate pair: import static imports.UnicodeImportType.ÉàAΩ\uD81D\uDE64Type; */

public class UnicodeImportType {
    {
        var l = List.class;
    }

    public static class ÉàAΩType {
    }
}
