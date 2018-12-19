package com.ambantis.akmeter

import akka.actor.{Actor, ActorLogging}

trait BaseActor extends Actor with ActorLogging {
  import BaseActor._

  override def unhandled(msg: Any): Unit = {
    log.warning(s"received unhandled message {} from {}", trimmed(msg), sender())
    super[Actor].unhandled(msg)
  }
}

object BaseActor {

  private val limit: Int = 200

  def trimmed(message: Any): String = {
    val toString = message.toString
    if (toString.length <= limit) toString
    else s"${toString.take(limit - 3)}..."
  }
}
