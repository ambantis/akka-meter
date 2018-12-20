package com.ambantis.akmeter
package db

import java.util.concurrent.TimeoutException

import scala.util.control.NoStackTrace

sealed trait DbException extends Exception with NoStackTrace

case object NotFoundException extends Exception("not found") with DbException {
  override val toString: String = "NotFoundException"
}

case object GatewayTimeout extends TimeoutException("time out") with DbException {
  override val toString: String = "TimeOutException"
}
