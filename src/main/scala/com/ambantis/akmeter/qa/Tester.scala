package com.ambantis.akmeter
package qa

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.time.Instant
import java.util.{Date, UUID}
import java.util.concurrent.{Executors, ScheduledExecutorService}
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntUnaryOperator

import scala.collection.AbstractIterator
import scala.concurrent.Await
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorRef, Props, Scheduler}
import akka.pattern.ask
import akka.util.Timeout
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.stream.scaladsl._
import io.grpc.{CallOptions, ManagedChannel, ManagedChannelBuilder}
import com.ambantis.akmeter.protos.{HashGrpc, HashReply, HashRequest}
import com.ambantis.akmeter.util._
import com.ambantis.akmeter.db.DbActor
import com.ambantis.akmeter.sim.{Simulator, SequentialSimulator}

object Tester {

  val name: String = "tester"

  def props(config: AppConfig, dbActor: ActorRef): Props = {
    val ip: InetSocketAddress = new InetSocketAddress("127.0.0.1", config.api.port)

    val channel: ManagedChannel =
      ManagedChannelBuilder
        .forAddress(ip.getHostString, ip.getPort)
        .usePlaintext(true)
        .build()
    val stub = new HashGrpc.HashStub(channel, CallOptions.DEFAULT)

    val client = new ApiClient[HashRequest, HashReply] {

      override def close(): Unit = channel.shutdown()

      override def execute(request: HashRequest)(implicit ec: ExecutionContext): Future[HashReply] =
        stub.computeHash(request)
    }


    Props(new Tester(config.qa, dbActor, client, requestGenerator))
  }

  val requestGenerator: Generator[HashRequest] =
    new Generator[HashRequest] {

      val xs = List(
        "aba",
        "action",
        "adventure",
        "anime",
        "award_winners_and_nominees",
        "black_cinema",
        "childrens",
        "classics",
        "comedy",
        "comedy-drama",
        "complexnetworks",
        "crime",
        "cult_favorites",
        "documentary",
        "drama",
        "epic",
        "family_movies",
        "fantasy",
        "foreign_favorites",
        "foreign_films",
        "highly_rated_on_rotten_tomatoes",
        "historical",
        "horror",
        "impact_channel",
        "indie_films",
        "kids_shows",
        "leaving_soon",
        "lgbt",
        "martial_arts",
        "most_popular",
        "movie_night",
        "musical",
        "mystery",
        "new_releases",
        "not_on_netflix",
        "preschool",
        "recently_added",
        "romance",
        "sci_fi_and_fantasy",
        "shoutfactory",
        "spanish_language",
        "special_interest",
        "spy",
        "stand_up_comedy",
        "thrillers",
        "trending",
        "tv_comedies",
        "war",
        "weekly_watchlist",
        "western",
        "A Santa at Nasa"
      ).map(s => HashRequest(s))

      override def requests(n: Int): List[HashRequest] = xs.take(n)
    }
}

class Tester(config: QaConfig,
             dbActor: ActorRef,
             client: ApiClient[HashRequest, HashReply],
             gen: Generator[HashRequest])
    extends BaseActor {
  import context.dispatcher

  val expected = new collection.mutable.HashMap[String, Int]()

  val metrics =
    new Metrics[HashRequest, HashReply] {
      def ok(implicit request: HashRequest): Unit = log.info("got ok")
      def unk(implicit request: HashRequest): Unit = log.info("got unk")
      def error(e: Throwable)(implicit request: HashRequest): Unit = log.info("got error {}", e)
      def inflightInc(): Unit = log.info("got inflight inc")
      def inflightDec(): Unit = log.info("got inflight dec")

      def latency(time: Long): Unit = log.info("got latency {}", time)

      def now(): Long = System.currentTimeMillis()

    }

  def validate(request: HashRequest, response: HashReply): Try[Option[Unit]] =
    Try(expected.get(request.body).map(hash => require(hash == response.hash, s"REQUIREMENTS FAILURE!! expected $hash != ${response.hash}")))

  override def preStart(): Unit = {
    log.info(s"simulation starting up")
    implicit val askTimeout: Timeout = 1.minute

    Await.result(
      Future.sequence {
        gen.requests(Int.MaxValue).map { request =>
          (dbActor ? DbActor.FailFreeCompute(request.body))
            .mapTo[Int]
            .map { hash =>
              expected.put(request.body, hash)
            }
        }
      },
      1.day
    )

    log.info(s"simulation seed completed")

  }

  val simulation: Simulator[HashRequest, HashReply] =
    SequentialSimulator(config, client, validate, gen, metrics)

  implicit val mat = ActorMaterializer()

  simulation.run.onComplete { result =>
    log.info(s"got result $result, shutting down...")
    context stop self
  }

  override def receive: Receive = Actor.emptyBehavior
}
