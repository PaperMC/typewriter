package imports;

import java.util.List; // root class import
import java.util.random.RandomGenerator.JumpableGenerator; // inner class import (directly referenced)
import java.util.*; // star import (whole package + one level)
import static javax.lang.model.SourceVersion.RELEASE_21; // static import
import static javax.lang.model.SourceVersion.*; // static star import

public class StandardImportType {

}
