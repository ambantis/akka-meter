package com.ambantis.akmeter
package util

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.language.implicitConversions

import akka.actor.Scheduler

final case class Deadline(epoch: EpochMillis, private[util] val timeout: FiniteDuration) {

  private val expiresAt: EpochMillis = new EpochMillis(epoch.value + timeout.toMillis)

  private[util] def millisRemaining(): Long = expiresAt.elapsed(EpochMillis())

  def elapsed(): FiniteDuration = EpochMillis().elapsed(epoch).millis

  def remaining(): FiniteDuration = {
    val delta = millisRemaining()

    if (delta > 0) delta.millis else Duration.Zero
  }

  def hasTimeLeft(): Boolean = millisRemaining() > 0

  def isOverdue(): Boolean = !hasTimeLeft()

  override def toString: String = s"Deadline($epoch, ${remaining()})"
}

object Deadline {

  def apply(timeout: FiniteDuration = Duration.Zero): Deadline = Deadline(EpochMillis(), timeout)

  def futureWithTimeout[T](
    fut: => Future[T]
  )(implicit ec: ExecutionContext, deadline: Deadline, sched: Scheduler): Future[T] =
    if (deadline.isOverdue()) Future.failed(db.GatewayTimeout)
    else {
      val p = Promise[T]()
      val cancellable = sched.scheduleOnce(deadline.remaining())(p.tryFailure(db.GatewayTimeout))
      p.tryCompleteWith(fut)
      p.future.onComplete(_ => cancellable.cancel())
      p.future
    }

  implicit def toDeadline(timeout: FiniteDuration): Deadline = Deadline(timeout)

  implicit def toDuration(deadline: Deadline): FiniteDuration = deadline.remaining

}
