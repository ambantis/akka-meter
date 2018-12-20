package com.ambantis.akmeter
package sim

import java.net.InetSocketAddress

import scala.concurrent.duration._

import com.typesafe.config.Config

/** Configuration for running Simulations.
 *
 */
class SimConfig(
  val optAddress: Option[InetSocketAddress],
  val duration: FiniteDuration,
  val opsPerSecond: Int,
  val rampUp: FiniteDuration,
  val userCount: Int,
  val timeout: FiniteDuration,
  val parallelism: Int
) {

  val rampFactor: Double = {
    val raw: Double = if (rampUp == Duration.Zero) 0.0 else opsPerSecond / rampUp.toSeconds.toDouble
    math.ceil(raw).toInt
  }
}

object SimConfig {

  def ipAddress(config: Config, at: String): Option[InetSocketAddress] =
    if (!config.hasPath(at)) None
    else Some(new InetSocketAddress(config.getString(s"$at.host"), config.getInt(s"$at.port")))

  def apply(config: Config, basePath: String): SimConfig = {
    val path = s"$basePath.sim"

    new SimConfig(
      optAddress = ipAddress(config, s"$path.endpoint"),
      duration = config.getDuration(s"$path.duration", SECONDS).seconds,
      opsPerSecond = config.getInt(s"$path.ops-per-second"),
      rampUp = config.getDuration(s"$path.ramp-up", SECONDS).seconds,
      userCount = config.getInt(s"$path.user-count"),
      parallelism = config.getInt(s"$path.parallelism"),
      timeout = config.getDuration(s"$path.timeout", MILLISECONDS).millis
    )
  }
}
