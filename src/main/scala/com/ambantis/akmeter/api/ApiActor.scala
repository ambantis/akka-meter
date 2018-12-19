package com.ambantis.akmeter
package api

import akka.actor._

object ApiActor extends Role {

  val name = "api"

  def props(cfg: AppConfig, db: ActorRef): Props =
    Props(new ApiActor(cfg, db))
}

class ApiActor(cfg: AppConfig, db: ActorRef) extends BaseActor {

  override def preStart(): Unit =
    log.info("api starting up ...")

  override def receive: Receive = Actor.emptyBehavior
}
