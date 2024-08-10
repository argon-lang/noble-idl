package nobleidl

import zio.*
import zio.stream.*

trait CodeWriter {
  def indent(): UIO[Unit]
  def dendent(): UIO[Unit]

  def write(s: String): UIO[Unit]
  def writeln(s: String): UIO[Unit]
  def writeln(): UIO[Unit]
}

object CodeWriter {

  def withWriter[R, E](op: ZIO[R & CodeWriter, E, Unit]): ZStream[R, E, String] =
    ZStream.fromZIO(Queue.bounded[Option[String]](10))
      .flatMap { queue =>
        ZStream.unwrap(
          for
            task <- op.onExit(_ => queue.offer(None)).fork.provideSomeLayer[R](liveFromQueue(queue))

          yield ZStream.fromQueue(queue).collectSome ++ ZStream.fromZIO(task.join).drain
        )
      }

  private def liveFromQueue(queue: Enqueue[Option[String]]): ULayer[CodeWriter] =
    ZLayer.fromZIO(
      for
        indentLevel <- Ref.make(0)
        atLineStart <- Ref.make(true)
      yield new CodeWriter {
        override def indent(): UIO[Unit] =
          indentLevel.update(_ + 1)

        override def dendent(): UIO[Unit] =
          indentLevel.update(level => if level > 0 then level - 1 else 0)

        override def write(s: String): UIO[Unit] =
          indentLevel.get.flatMap { level =>
            queue.offer(Some(s * level)) *>
              atLineStart.set(false)
          } *> queue.offer(Some(s)).unit

        override def writeln(s: String): UIO[Unit] =
          write(s) *> writeln()

        override def writeln(): UIO[Unit] =
          System.lineSeparator.flatMap(sep => queue.offer(Some(sep))) *>
            atLineStart.set(true)
      }
    )

  object Operations {
    def indent(): URIO[CodeWriter, Unit] = ZIO.serviceWithZIO(_.indent())
    def dendent(): URIO[CodeWriter, Unit] = ZIO.serviceWithZIO(_.dendent())

    def write(s: String): URIO[CodeWriter, Unit] = ZIO.serviceWithZIO(_.write(s))
    def writeln(s: String): URIO[CodeWriter, Unit] = ZIO.serviceWithZIO(_.writeln(s))
    def writeln(): URIO[CodeWriter, Unit] = ZIO.serviceWithZIO(_.writeln())
  }
}
