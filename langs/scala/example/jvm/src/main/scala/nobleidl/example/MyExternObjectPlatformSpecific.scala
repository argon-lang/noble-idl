package nobleidl.example

import nobleidl.core.JavaAdapter

trait MyExternObjectPlatformSpecific {
  def javaAdapter(): JavaAdapter[MyExtern, dev.argon.nobleidl.example.MyExtern] =
    new JavaAdapter[MyExtern, dev.argon.nobleidl.example.MyExtern] {
      override def toJava(s: MyExtern): dev.argon.nobleidl.example.MyExtern =
        dev.argon.nobleidl.example.MyExtern()

      override def fromJava(j: dev.argon.nobleidl.example.MyExtern): MyExtern =
        MyExtern()
    }
}
