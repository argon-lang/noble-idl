package nobleidl.compiler.api

extension (obj: PackageName.type)
  def fromString(s: String): PackageName =
    if s.isEmpty then
      PackageName(Seq())
    else
      PackageName(s.split("\\.").nn.view.map(_.nn).toSeq)

extension (packageName: PackageName)
  def display: String =
    packageName.parts.mkString(".")
