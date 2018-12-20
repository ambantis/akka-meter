package com.ambantis.akmeter
package api

import java.io.Closeable
import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{Deadline => _, _}
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

import akka.actor.{ActorContext, ActorRef, Scheduler}
import akka.pattern.ask
import akka.util.Timeout
import akka.event.{Logging, LoggingAdapter}
import com.ambantis.akmeter.protos.{HashGrpc, HashReply, HashRequest}
import com.ambantis.akmeter.db.{GatewayTimeout, NotFoundException}
import com.ambantis.akmeter.util.Converters.FutureOps
import com.ambantis.akmeter.util.{Deadline, EpochMillis}
import io.grpc._
import io.grpc.Server

class GrpcHashService(dbActor: ActorRef, config: ApiConfig, log: LoggingAdapter)(
  implicit ec: ExecutionContext,
  scheduler: Scheduler
) extends HashGrpc.Hash {

  override def computeHash(req: HashRequest): Future[HashReply] = {
    implicit val context = Context.current()
    log.info(s"got request $req with context $context")

    val contextTimeout: FiniteDuration =
      context.getDeadline match {
        case null => 1.day
        case d    => d.timeRemaining(MILLISECONDS).millis - config.timeoutPad
      }

    implicit val deadline: Deadline = {
      def instant =
        ContextValue.XRequestTimestamp.contextValue
          .map(Instant.parse)
          .getOrElse(Instant.now())
      val timeout = config.timeout.min(contextTimeout)
      Deadline(EpochMillis(instant), timeout)
    }

    val id = ContextValue.XRequestId.contextValue.getOrElse("_" + UUID.randomUUID().toString)

    implicit val askTimeout: Timeout = deadline.remaining() + config.timeoutPad

    if (EmptyString.invalid(req.body)) Future.failed(statusEx(id, EmptyString))
    else {
      val result: Future[Try[Option[Int]]] =
        Deadline.futureWithTimeout {
          (dbActor ? req.body).mapTo[Option[Int]]
        }.wrapped

      result.flatMap {
        case Success(Some(hashCode)) =>
          Future.successful(HashReply(hashCode))
        case Success(None) =>
          Future.failed(statusEx(id, GatewayTimeout))
        case Failure(e) =>
          Future.failed(statusEx(id, e))
      }
    }
  }

  def statusEx(id: String, e: Throwable): StatusException = {
    def message: String = s"[$id] ${e.getMessage}"

    def to(status: Status): StatusException =
      status.withCause(e).withDescription(message).asException()

    e match {
      case _: RequestValidation =>
        to(Status.INVALID_ARGUMENT)

      case NotFoundException =>
        to(Status.NOT_FOUND)

      case GatewayTimeout =>
        to(Status.DEADLINE_EXCEEDED)

      case _ =>
        to(Status.INTERNAL)
    }
  }
}

object GrpcHashService {

  def apply(dbActor: ActorRef, config: ApiConfig)(implicit context: ActorContext): GrpcHashService = {
    val log = Logging(context.system.eventStream, classOf[GrpcHashService])
    new GrpcHashService(dbActor, config, log)(context.dispatcher, context.system.scheduler)
  }
}

abstract class RequestValidation(_message: String) extends Exception(_message)

case object EmptyString extends RequestValidation("Empty String") {
  def invalid(msg: String): Boolean = msg == null || msg.isEmpty()
}
