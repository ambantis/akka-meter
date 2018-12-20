package com.ambantis.akmeter
package sim

import java.net.InetSocketAddress

import scala.concurrent.duration._

import com.typesafe.config.Config

/** Configuration for running Simulations.
 *
 */
trait SimConfig {
  def duration: FiniteDuration
  def opsPerSecond: Int
  def rampUp: FiniteDuration
  def requestCount: Int
  def timeout: FiniteDuration
  def parallelism: Int

  def rampFactor: Double = {
    val raw: Double = if (rampUp == Duration.Zero) 0.0 else opsPerSecond / rampUp.toSeconds.toDouble
    math.ceil(raw).toInt
  }
}
