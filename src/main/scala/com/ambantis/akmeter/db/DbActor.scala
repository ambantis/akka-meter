package com.ambantis.akmeter
package db

import akka.actor._

object DbActor {

  val name: String = "db"

  def props(): Props = Props(new DbActor)
}

class DbActor extends BaseActor {

  override def preStart(): Unit =
    log.info("db starting up ...")

  override def receive: Receive = Actor.emptyBehavior

}
