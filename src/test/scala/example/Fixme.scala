package example

import org.scalatest.freespec.AsyncFreeSpec
import cats.effect._
import cats.effect.laws.util.TestContext

import java.util.concurrent.{
  Callable,
  Executors,
  Future,
  ScheduledExecutorService,
  ScheduledFuture,
  ScheduledThreadPoolExecutor,
  ThreadFactory,
  TimeUnit,
}
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class Fixme extends AsyncFreeSpec {
  "ad" in {
    val tc = TestContext()
    val cs = tc.ioContextShift
    val timer = tc.ioTimer

    val m = mutable.ArrayBuffer.empty[Int]

    def myio: IO[Unit] =
      for {
        _ <- IO {
          println(s"Incrementing in $curThreadName")
          m += 1
        }
        _ <- IO.sleep(1.seconds)(timer)
        _ <- myio
      } yield ()

    // Make sure all evaluation is run (controlled) by the TestContext
    cs.evalOn(tc)(myio).unsafeRunAsyncAndForget()

    val scheduler =
      ExecutionContext.fromExecutorService(
        new ScheduledThreadPoolExecutor(4, new ThreadFactory {
          override def newThread(r: Runnable): Thread = {
            val t =
              new Thread(
                Thread.currentThread().getThreadGroup,
                r,
                "schedule-thread",
                0,
              )
            if (t.isDaemon) t.setDaemon(false)
            if (t.getPriority != Thread.NORM_PRIORITY)
              t.setPriority(Thread.NORM_PRIORITY)
            t
          }
        }) {
          override def submit[T](task: Callable[T]): Future[T] = {
            println("Submit callable")
            super.submit(task)
          }

          override def submit(task: Runnable): Future[_] = {
            println("Sub runnabl")
            super.submit(task)
          }

          override def submit[T](task: Runnable, result: T): Future[T] = {
            println("Sub runnable res")
            super.submit(task, result)
          }

          override def schedule(
            command: Runnable,
            delay: Long,
            unit: TimeUnit,
          ): ScheduledFuture[_] = {
            println(s"sch Runnable $delay $unit")
            super.schedule(command, delay, unit)
          }
        },
      )

    val blocker: Blocker =
      Blocker.liftExecutionContext(scheduler)

    cs.evalOn(scheduler)(for {
        // Comment this line out and tests pass (tc.tick runs myio correct)
        _ <- blocker.delay[IO, Unit] {
          println(s"println in blocker $curThreadName")
        }(
          IO.ioEffect,
          IO.contextShift(scheduler),
        )
        _ <- IO {
          println(s"starting ticking TestContext $curThreadName")
          tc.tick(1.second)
          tc.tick(1.second)
          tc.tick(1.second)
        }
      } yield {
        println(m.toList)
        assert(m.nonEmpty)
        succeed
      })
      .unsafeToFuture()
  }

  private def curThreadName =
    Thread.currentThread().getName
}
