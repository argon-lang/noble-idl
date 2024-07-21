package dev.argon.nobidl.sjs.test

import dev.argon.nobidl.sjs.core.JSTypeAdapter

final class MyExtern
object MyExtern {
  def jsTypeAdapter: JSTypeAdapter[dev.argon.nobidl.scala_zio.test.MyExtern, MyExtern] =
    new JSTypeAdapter[dev.argon.nobidl.scala_zio.test.MyExtern, MyExtern] {
      override def toJS(a: dev.argon.nobidl.scala_zio.test.MyExtern): MyExtern = MyExtern()
      override def fromJS(b: MyExtern): dev.argon.nobidl.scala_zio.test.MyExtern = dev.argon.nobidl.scala_zio.test.MyExtern()
    }
}

final class MyExtern2[A]
