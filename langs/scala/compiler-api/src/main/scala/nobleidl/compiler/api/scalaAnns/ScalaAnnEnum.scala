package nobleidl.compiler.api.scalaAnns
enum ScalaAnnEnum derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.constructor("companion-extends")
  case CompanionExtends(
    `type`: _root_.nobleidl.core.String,
  )
}
object ScalaAnnEnum {
}
