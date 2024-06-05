package name;

public class SelfInnerClass {
    public class A {
        public class B {
            public class C {
                {
                    var a = A.class;
                    var b = B.class;
                    var c = C.class;
                }
            }
        }
    }

    {
        var a = A.class;
        var b = A.B.class;
        var c = A.B.C.class;
    }
}
