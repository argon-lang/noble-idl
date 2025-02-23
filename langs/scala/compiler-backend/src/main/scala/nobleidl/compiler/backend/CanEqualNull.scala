package nobleidl.compiler.backend

private[compiler] given canEqualNullHelper: [A] => CanEqual[A | Null, Null] = CanEqual.derived
 
