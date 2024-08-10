package nobleidl

import zio.{ExitCode, Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

abstract class PlatformAppBase extends ZIOAppDefault {
  final override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    runImpl.flatMap(exit)
  
  def runImpl: ZIO[ZIOAppArgs & Scope, Any, ExitCode]
  
}
