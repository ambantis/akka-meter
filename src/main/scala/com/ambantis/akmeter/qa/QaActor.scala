package com.ambantis.akmeter
package qa

import akka.actor.Terminated
import scala.collection.JavaConverters._

import akka.actor._
import io.grpc._
import com.ambantis.akmeter.protos.HashGrpc

object QaActor extends Role {

  val name = "qa"

  def props(cfg: AppConfig, db: ActorRef): Props =
    Props(new QaActor(cfg, db))
}

class QaActor(cfg: AppConfig, db: ActorRef) extends BaseActor {
  import context.actorOf

  val tester = actorOf(Tester.props(cfg, db), Tester.name)

  override def receive: Receive = {
    case Terminated(_) =>
      context stop self
  }

}
