package com.ambantis.akmeter
package qa

import scala.collection.JavaConverters._

import akka.actor._
import io.grpc._
import com.ambantis.akmeter.protos.HashGrpc

object QaActor extends Role {

  val name = "api"

  def props(cfg: AppConfig, db: ActorRef): Props =
    Props(new QaActor(cfg, db))
}

class QaActor(cfg: AppConfig, db: ActorRef) extends BaseActor {

  override def receive: Receive = Actor.emptyBehavior

}
