package com.ambantis.akmeter
package util

import java.io.Closeable

import scala.concurrent.{ExecutionContext, Future}

abstract class ApiClient[T, U] extends Closeable {

  def execute(req: T)(implicit ec: ExecutionContext): Future[U]

}
