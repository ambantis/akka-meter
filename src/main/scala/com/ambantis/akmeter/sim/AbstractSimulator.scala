package com.ambantis.akmeter
package sim

import java.io.Closeable
import java.util.concurrent.{Executors, ScheduledExecutorService, ScheduledFuture}

import scala.collection.AbstractIterator
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.ambantis.akmeter.util.Converters.FutureOps

class AbstractSimulator[T, U](
  config: SimConfig,
  client: ApiClient[T, U],
  requestFun: => T,
  validateFun: (T, U) => Try[Option[Unit]],
  generator: Generator[T],
  metrics: SimMetrics[T, U]
) {

  @volatile private var currentN: Double = 0

  // since this value is accessed once per second by a single threaded executor,
  // we should not have to worry about multiple threads calling this method at
  // the same time
  def nextN(): Int = {
    val next = math.min(config.opsPerSecond, currentN + config.rampFactor)
    currentN = next
    next.toInt
  }

  val requests: Iterator[T] = new AbstractIterator[T] {
    val vec: Vector[T] = generator.requests(config.userCount).toVector

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

  def run(implicit mat: ActorMaterializer): Future[Boolean] = {
    val p = Promise[Boolean]()

    val es: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    val scheduledFuture: ScheduledFuture[_] =
      es.scheduleAtFixedRate(new Runnable {
        override def run(): Unit = {
          val n = nextN()
          callApi(n)
        }
      }, /* initialDelay = */ 10, /* delay = */ 1, SECONDS)

    es.schedule(new Runnable {
      override def run(): Unit = p.tryComplete(Try(scheduledFuture.cancel(false)))
    }, config.duration.length, config.duration.unit)

    p.future.andThen { case _ => es.shutdown() }(mat.executionContext)
  }
}

abstract class ApiClient[T, U] extends Closeable {

  def execute(req: T)(implicit ec: ExecutionContext): Future[U]

}

abstract class Generator[T] {

  def requests(n: Int): List[T]

}
