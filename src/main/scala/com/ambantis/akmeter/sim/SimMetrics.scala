package com.ambantis.akmeter
package sim

import scala.util.{Failure, Success, Try}

trait SimMetrics[T, U] {

  def ok(implicit request: T): Unit
  def unk(implicit request: T): Unit
  def error(e: Throwable)(implicit request: T): Unit
  def inflightInc(): Unit
  def inflightDec(): Unit

  def latency(time: Long): Unit

  def report(result: Try[Option[_]])(implicit start: Long, request: T): Unit = {
    latency(now() - start)
    inflightDec()
    result match {
      case Success(Some(_)) => ok
      case Success(None)    => unk
      case Failure(e)       => error(e)
    }
  }

  def now(): Long
}
