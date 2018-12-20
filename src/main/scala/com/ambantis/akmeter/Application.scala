package com.ambantis.akmeter

import scala.concurrent.duration._

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import com.typesafe.config.ConfigFactory
import com.ambantis.akmeter.api.ApiActor
import com.ambantis.akmeter.db.DbActor

object Application extends App {

  val config = ConfigFactory.load()

  val system = ActorSystem("akmeter", config)

  system.registerOnTermination(System.exit(0))

  system.actorOf(Props(new Application(AppConfig(config))), "app")
}

class Application(appConfig: AppConfig) extends BaseActor {
  import context.{actorOf, dispatcher, watch}

  val db = watch(initDbClient())
  def initDbClient(): ActorRef = actorOf(DbActor.props(appConfig.db), DbActor.name)

  val roles = appConfig.roles.map(role => watch(actorOf(role.props(appConfig, db), role.name)))

  override def preStart(): Unit =
    log.info("hello world")

  override def postStop(): Unit =
    context.system.terminate()

  override def receive: Receive = {
    case Terminated(_) =>
      context.stop(self)
  }

}
