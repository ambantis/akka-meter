package com.ambantis.akmeter
package util

import java.time.{Instant, ZoneId}

import com.google.protobuf.timestamp.Timestamp

class EpochMillis(val value: Long) extends AnyVal with Ordered[EpochMillis] {
  def elapsed(that: EpochMillis): Long = value - that.value
  override def toString: String = value.toString
  override def compare(that: EpochMillis): Int = value.compare(that.value)

}

object EpochMillis {

  final val Zero = new EpochMillis(0)

  final val UTC: ZoneId = ZoneId.of("UTC")

  def apply(): EpochMillis = new EpochMillis(System.currentTimeMillis())

  def apply(instant: Instant): EpochMillis = {
    val utc = instant.atZone(UTC).toInstant
    val milliseconds: Long = utc.toEpochMilli()
    if (milliseconds == 0) Zero
    else new EpochMillis(milliseconds)
  }

  def apply(timestamp: Timestamp): EpochMillis = {
    val seconds = timestamp.seconds
    val nanos = timestamp.nanos
    if (seconds == 0 && nanos == 0) Zero
    else new EpochMillis(Instant.ofEpochSecond(seconds, nanos).toEpochMilli())
  }
}
