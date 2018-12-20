package com.ambantis.akmeter
package util

import java.util.concurrent.Executor

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try
import scala.util.control.NonFatal

object Converters {

  /** Wraps ExecutionContext into java.concurrent.Executor
   *
   * Inspired by https://gist.github.com/alexandru/5244811
   */
  implicit def toExecutor(implicit ec: ExecutionContext): Executor =
    new Executor {
      def execute(command: Runnable): Unit =
        ec.execute(new Runnable {
          def run(): Unit =
            try command.run()
            catch { case NonFatal(ex) => ec.reportFailure(ex) }
        })
    }

  implicit class FutureOps[T](fut: Future[T]) {
    def wrapped(implicit ec: ExecutionContext): Future[Try[T]] = {
      val p: Promise[Try[T]] = Promise[Try[T]]()
      fut.onComplete(p.success)
      p.future
    }
  }
}
