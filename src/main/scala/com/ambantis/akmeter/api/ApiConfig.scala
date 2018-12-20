package com.ambantis.akmeter
package api

import scala.concurrent.duration._

import com.typesafe.config.Config

final case class ApiConfig(port: Int, timeout: FiniteDuration, timeoutPad: FiniteDuration)

object ApiConfig {

  def apply(config: Config, basePath: String): ApiConfig = {
    val path: String = s"$basePath.api"
    ApiConfig(
      port = config.getInt(s"$path.port"),
      timeout = config.getDuration(s"$path.timeout", MILLISECONDS).millis,
      timeoutPad = config.getDuration(s"$path.timeout-pad", MILLISECONDS).millis,
    )

  }
}
