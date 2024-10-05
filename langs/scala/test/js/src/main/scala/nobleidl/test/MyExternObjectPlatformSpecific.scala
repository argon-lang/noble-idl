package nobleidl.test

import nobleidl.core.JSAdapter

trait MyExternObjectPlatformSpecific {
  def jsAdapter(): JSAdapter[MyExtern, nobleidl.sjs.test.MyExtern] =
    new JSAdapter[MyExtern, nobleidl.sjs.test.MyExtern] {
      override def toJS(s: MyExtern): nobleidl.sjs.test.MyExtern =
        new nobleidl.sjs.test.MyExtern {}

      override def fromJS(j: nobleidl.sjs.test.MyExtern): MyExtern =
        MyExtern()
    }
}
