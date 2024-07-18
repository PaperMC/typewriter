package name;

import name.SelfInnerClass.D;

public class SelfInnerClass {
    public class A {
        public class B {
            public class C {
                {
                    var a = A.class;
                    var b = B.class;
                    var c = C.class;
                    var d = D.class;
                }
            }

            public class E {
                {
                    var a = A.class;
                    var b = B.class;
                    var c = C.class;
                    var d = D.class;
                    var f = D.F.class;
                }
            }
        }
    }

    public class D {
        {
            var a = A.class;
            var b = A.B.class;
            var c = A.B.C.class;
            var d = D.class;
            var f = F.class;
        }

        public class F {

        }
    }

    {
        var a = A.class;
        var b = A.B.class;
        var c = A.B.C.class;
        var d = D.class;
        var f = D.F.class;
    }
}
