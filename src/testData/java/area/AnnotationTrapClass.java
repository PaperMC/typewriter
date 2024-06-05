package area;

@AnnotationTrapClass.Trapped
    (
    /* ) { */  // ) {
    /* ) { */
    strings = {"trap: )   \" )  { // /* "}, annotation = @AnnotationTrapClass.Trapped2(strings = {"trap: )   \" )  {"}, chars = {')', '{', '\''}, paragraphs = """
    )
    \"\"\"
    {
    """, clazz = AnnotationTrapClass/* ) { */ ./* ) { // */ Trapped.class/* ) { */ ) /* ) { */ , chars = {')', '{', '\''}, paragraphs = /* ) { */ // ) {
        """
        )
        \"\"\"
        {
        """
    /* ) { */ // ) {
    /* ) { */
    )
public class AnnotationTrapClass { // << 33

    @interface Trapped {
        String[] strings();

        char[] chars();

        String[] paragraphs();

        Trapped2 annotation();
    }

    @interface Trapped2 {

        String[] strings();

        char[] chars();

        String[] paragraphs();

        Class<? extends Trapped> clazz();

    }
}
