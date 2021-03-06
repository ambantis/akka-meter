package com.ambantis.akmeter
package sim

import java.io.Closeable
import java.util.concurrent.{Executors, ScheduledExecutorService, ScheduledFuture}

import scala.collection.AbstractIterator
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.ambantis.akmeter.util.Converters.FutureOps
import com.ambantis.akmeter.util.{ApiClient, Generator, Metrics}

abstract class Simulator[T, U] extends Closeable {

  def run(implicit mat: ActorMaterializer): Future[Boolean]

}

final case class SequentialSimulator[T, U](
  config: SimConfig,
  client: ApiClient[T, U],
  validateFun: (T, U) => Try[Option[Unit]],
  generator: Generator[T],
  metrics: Metrics[T, U]
) extends Simulator[T, U] {

  val es: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

  @volatile private var currentN: Double = 0

  var optFuture: Option[ScheduledFuture[_]] = None

  def canBeRun: Boolean = !es.isShutdown && optFuture.isEmpty

  // run can only be run once
  override def run(implicit mat: ActorMaterializer): Future[Boolean] = synchronized {
    import mat.executionContext
    val p = Promise[Boolean]()

    if (!canBeRun) p.tryFailure(new IllegalArgumentException("simulation already started"))
    else {
      val scheduledFuture =
        es.scheduleAtFixedRate(new Runnable {
          def run(): Unit = {
            val n = nextN()
            callApi(n)
          }
        }, /* initialDelay = */ 10, /* delay = */ 1, SECONDS)

      // stop the simulation when the simulation duration expires
      es.schedule(new Runnable {
        def run(): Unit = p.tryComplete(Try(scheduledFuture.cancel(false)))
      }, config.duration.length, config.duration.unit)

      optFuture = Some(scheduledFuture)

      p.future.onComplete { _ =>
        currentN = 0
        optFuture = None
      }
    }

    p.future
  }

  override def close(): Unit = es.shutdown()

  // since this value is accessed once per second by a single threaded executor,
  // we should not have to worry about multiple threads calling this method at
  // the same time
  def nextN(): Int = {
    val next = math.min(config.opsPerSecond, currentN + config.rampFactor)
    currentN = next
    next.toInt
  }

  val requests: Iterator[T] = new AbstractIterator[T] {
    val vec: Vector[T] = generator.requests(config.requestCount).toVector

    val zero = 0
    val max = vec.length - 1

    var index = zero

    def hasNext: Boolean = true
    def next(): T = {
      val next = vec(index)
      if (index == max) index = zero
      else index += 1

      next
    }
  }

  val source: Source[T, NotUsed] = Source.cycle(() => requests)

  def callApi(n: Int)(implicit mat: ActorMaterializer): Unit = {
    import mat.executionContext
    source
      .take(n)
      .mapAsyncUnordered(config.parallelism) { implicit request =>
        implicit val start = metrics.now()
        metrics.inflightInc()
        client
          .execute(request)
          .wrapped
          .andThen {
            case Success(Success(response)) =>
              metrics.report(validateFun(request, response))
            case Success(Failure(e)) =>
              metrics.report(Failure(e))
          }

      }
      .runWith(Sink.ignore)
  }

}
