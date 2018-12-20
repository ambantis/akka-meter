package com.ambantis.akmeter
package db

import scala.concurrent.duration._

import com.typesafe.config.Config

final case class DbConfig(min: FiniteDuration, max: FiniteDuration, successRate: Double, foundRate: Double) {
  val range: FiniteDuration = max - min
  require(min > Duration.Zero && max > Duration.Zero, "min/max must be positive durations")
  require(range > Duration.Zero, s"range $range not greater than zero for $this")
  require(successRate >= 0 && successRate <= 1)
  require(foundRate >= 0 && foundRate <= 1)
}

object DbConfig {

  def apply(config: Config, basePath: String): DbConfig = {
    val path = s"$basePath.db"
    DbConfig(
      min = config.getDuration(s"$path.min", MILLISECONDS).millis,
      max = config.getDuration(s"$path.max", MILLISECONDS).millis,
      successRate = config.getDouble(s"$path.success-rate"),
      foundRate = config.getDouble(s"$path.found-rate")
    )
  }

}
