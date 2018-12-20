package com.ambantis.akmeter
package db

import scala.concurrent.duration._

import akka.actor.{ActorRef, Props, Status}

object DbActor {

  val name: String = "db"

  def props(config: DbConfig): Props = Props(new DbActor(config))
}

class DbActor(config: DbConfig) extends BaseActor {
  import DbActor._
  import context.{dispatcher, system}

  val rand = new java.util.Random()

  override def preStart(): Unit =
    log.info("db starting up ...")

  override def receive: Receive = {
    case any: Any => handle(any, sender())
  }

  def handle(any: Any, originalSender: ActorRef): Unit = {
    def isFailure(): Boolean = rand.nextDouble() > config.successRate
    def notFound(): Boolean = rand.nextDouble() > config.foundRate

    val delay: FiniteDuration = (rand.nextDouble() * config.range + config.min).length.nanos

    val result: Status.Status =
      isFailure() match {
        case true                => Status.Failure(new Exception("boom"))
        case false if notFound() => Status.Success(None)
        case _                   => Status.Success(Some(any.hashCode))
      }

    system.scheduler.scheduleOnce(delay, originalSender, result)(dispatcher)
  }
}
