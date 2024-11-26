package name.one;

public class OneDepthClass {

    public static class StaticClass {
        public static class Inner {
            public class Inner2 {

            }
        }
    }

    public static class StaticClass2 {
        public static class Inner {
            public class Inner2 {

            }
        }
    }

    public class NonStaticClass {
        public class Inner {

        }
    }

    public class NonStaticClass2 {

    }
}
