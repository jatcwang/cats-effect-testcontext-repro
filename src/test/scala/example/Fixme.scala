package example

import org.scalatest.freespec.AsyncFreeSpec
import cats.effect._
import cats.effect.laws.util.TestContext

import java.util.concurrent.Executors
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class Fixme extends AsyncFreeSpec {
  val execContextOfScalatest: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newScheduledThreadPool(4))

  "hmm" in {
    val tc = TestContext()
    val testContextCs = tc.ioContextShift
    val testContextTimer = tc.ioTimer

    val m = mutable.ArrayBuffer.empty[Int]

    def myio: IO[Unit] =
      for {
        _ <- IO.sleep(100.millis)(testContextTimer)
        _ <- IO {
          println(s"Incrementing in $curThreadName")
          m += 1
        }
        _ <- myio
      } yield ()

    // Make sure all evaluation is run (controlled) by the TestContext
    testContextCs.evalOn(tc)(myio).unsafeRunAsyncAndForget()

    val blocker: Blocker = Blocker.liftExecutionContext(execContextOfScalatest)

    (for {
      // Comment this expression out and tests pass (tc.tick runs myio correct)
      // The other way to fix this is to add IO.shift(execContextOfScalatest) after this expression
      _ <- blocker.delay[IO, Unit] {
        println(s"println in blocker $curThreadName")
      }(
        IO.ioEffect,
        IO.contextShift(execContextOfScalatest),
      )
      _ <- IO {
        println(s"Running TestContext.tick in $curThreadName")
        println(tc.state) // First Runnable scheduled, good
        tc.tick(100.millis)
        println(tc.state) // Why is there no Runnable scheduled to be run??
        tc.tick(100.millis)
        tc.tick(100.millis)
        tc.tick(100.millis)
      }
    } yield {
      println(m.toList)
      assert(m.nonEmpty)
      succeed
    }).unsafeToFuture()
  }

  private def curThreadName =
    Thread.currentThread().getName
}
