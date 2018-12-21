package com.ambantis.akmeter
package db

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.{ActorRef, Props, Status}
import akka.pattern.pipe

object DbActor {

  val name: String = "db"

  def props(config: DbConfig): Props = Props(new DbActor(config))

  final case class FailFreeCompute(s: String)
}

class DbActor(config: DbConfig) extends BaseActor {
  import DbActor._
  import context.{dispatcher, system}

  val rand = new java.util.Random()

  override def preStart(): Unit =
    log.info("db starting up ...")

  override def receive: Receive = {
    case s: String => handle(s, sender())

    case FailFreeCompute(s) =>
      val originalSender = sender()
      Future.successful(computeHash(s)).pipeTo(originalSender)
  }

  def handle(s: String, originalSender: ActorRef): Unit = {
    def isFailure(): Boolean = rand.nextDouble() > config.successRate
    def notFound(): Boolean = rand.nextDouble() > config.foundRate

    val delay: FiniteDuration = (rand.nextDouble() * config.range + config.min).length.nanos
    val result: Status.Status =
      isFailure() match {
        case true                => Status.Failure(new Exception("boom"))
        case false if notFound() => Status.Success(None)
        case _                   => Status.Success(Some(computeHash(s)))
      }

    system.scheduler.scheduleOnce(delay, originalSender, result)(dispatcher)
  }

  def isPalindrome(s: String): Boolean = {
    val raw = s.toLowerCase().replaceAll("[^A-Za-z0-9]", "")
    raw == raw.reverse
  }

  def computeHash(s: String): Int = {
    val hash = s.hashCode()
    val sign: Int = if (hash < 0) -1 else 1
    if (isPalindrome(s)) math.abs(hash).toString.reverse.toInt * sign
    else hash
  }
}
