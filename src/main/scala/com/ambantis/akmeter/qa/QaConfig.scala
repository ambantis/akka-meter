package com.ambantis.akmeter
package qa

import com.typesafe.config.Config
import scala.concurrent.duration._

import com.ambantis.akmeter.sim.SimConfig

final case class QaConfig(
  duration: FiniteDuration,
  opsPerSecond: Int,
  rampUp: FiniteDuration,
  requestCount: Int,
  timeout: FiniteDuration,
  parallelism: Int
) extends SimConfig

object QaConfig {

  def apply(config: Config, basePath: String): QaConfig = {
    val path = s"$basePath.qa"
    QaConfig(
      duration = config.getDuration(s"$path.duration", MILLISECONDS).millis,
      opsPerSecond = config.getInt(s"$path.ops-per-second"),
      rampUp = config.getDuration(s"$path.ramp-up", MILLISECONDS).millis,
      requestCount = config.getInt(s"$path.request-count"),
      timeout = config.getDuration(s"$path.timeout", MILLISECONDS).millis,
      parallelism = config.getInt(s"$path.parallelism")
    )
  }
}
