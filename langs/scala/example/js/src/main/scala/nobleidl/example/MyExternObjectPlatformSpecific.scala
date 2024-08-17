package nobleidl.example

import nobleidl.core.JSAdapter

trait MyExternObjectPlatformSpecific {
  def jsAdapter(): JSAdapter[MyExtern, nobleidl.sjs.example.MyExtern] =
    new JSAdapter[MyExtern, nobleidl.sjs.example.MyExtern] {
      override def toJS(s: MyExtern): nobleidl.sjs.example.MyExtern =
        new nobleidl.sjs.example.MyExtern {}

      override def fromJS(j: nobleidl.sjs.example.MyExtern): MyExtern =
        MyExtern()
    }
}
