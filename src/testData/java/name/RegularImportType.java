package name;

import java.util.List;
import static javax.lang.model.SourceVersion.RELEASE_21;
import static javax.lang.model.SourceVersion.valueOf;

public class RegularImportType {
    {
        var a = List.of();
        var b = RELEASE_21;
        valueOf(b.name());
    }
}
