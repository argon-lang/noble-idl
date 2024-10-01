package nobleidl.compiler

private[compiler] given canEqualNullHelper[A]: CanEqual[A | Null, Null] = CanEqual.derived
 
