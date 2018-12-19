package com.ambantis.akmeter

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

import akka.actor.Actor

object Application extends App {

  val config = ConfigFactory.load()

  val system = ActorSystem("akmeter", config)

  system.registerOnTermination(System.exit(0))

  system.actorOf(Props(new Application(AppConfig(config))))



}

class Application(cfg: AppConfig) extends BaseActor {
  import context.dispatcher

  override def preStart(): Unit = {
    log.info("hello world")
    context.system.scheduler.scheduleOnce(10.seconds)(context.stop(self))
  }

  override def postStop(): Unit = {
    log.info("shutting down now")
    context.system.terminate()
  }

  override def receive: Receive = Actor.emptyBehavior

}
