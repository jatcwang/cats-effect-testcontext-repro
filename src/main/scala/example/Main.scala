package example
import cats.effect.laws.util.TestContext
import cats.effect.IO

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
import java.util.concurrent.{Executors, ThreadFactory}
import java.util.concurrent.atomic.AtomicInteger

object Main {

  def main(args: Array[String]): Unit = {
    val ec1: ExecutionContext = createScheduledEC(4, "ec1")
    val ec2: ExecutionContext = createScheduledEC(4, "ec2")

    val tc = TestContext()
    val testContextCs = tc.ioContextShift

    val atomicInt = new AtomicInteger(0)

    val baseInstant = System.nanoTime()

    val atomicAdd: IO[Unit] = IO {
      println(
        s"Calling atomicInt.addAndGet on thread ${curThreadName()} at ${System
          .nanoTime() - baseInstant}",
      )
      val _ = atomicInt.addAndGet(1)
    }

    def myio: IO[Unit] = atomicAdd

    // Make sure all evaluation is run (controlled) by the TestContext
    testContextCs.evalOn(tc)(myio).unsafeRunAsyncAndForget()

    val ec1Shift = IO.contextShift(ec1)

    (for {
      // Comment this expression out and tests pass (tc.tick runs myio correct)
      // The other way to fix this is to add IO.shift(sch) after this expression
      _ <- ec1Shift.evalOn(ec2)(IO {
        println(s"task meant for ec2 running on ${curThreadName()}")
      })
      _ <- IO {
        println(s"Running TestContext.tick in ${curThreadName()}")
        println(s"State of test context before tick: ${tc.state}")
        tc.tick()
        println(s"Tick done at ${System.nanoTime() - baseInstant}")
      }
    } yield {
      assert(atomicInt.get() > 0)
    }).unsafeRunSync()
  }

  private def curThreadName() =
    Thread.currentThread().getName

  private def createScheduledEC(
    numThreads: Int,
    poolName: String,
  ): ExecutionContextExecutorService = {
    ExecutionContext.fromExecutorService(
      Executors.newScheduledThreadPool(
        numThreads,
        new ThreadFactory() {
          val threadId = new AtomicInteger()

          override def newThread(r: Runnable): Thread = {
            val t = Executors.defaultThreadFactory.newThread(r)
            t.setName(s"$poolName-${threadId.getAndAdd(1)}")
            t.setDaemon(true)
            t
          }
        },
      ),
    )
  }

}
