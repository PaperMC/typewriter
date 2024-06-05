package area;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({
    ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.PACKAGE
})
@AnnotationClass.NestedAnnot@AnnotationClass.NestedAnnot2
public @interface AnnotationClass { // << 34
    // @interface should be invalidated and not detected as an annotation
    // via its keyword: interface

    @interface NestedAnnot {

    }

    @interface NestedAnnot2 {

    }
}
