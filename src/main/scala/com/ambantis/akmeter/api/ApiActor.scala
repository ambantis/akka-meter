package com.ambantis.akmeter
package api

import scala.collection.JavaConverters._

import akka.actor._
import io.grpc._
import com.ambantis.akmeter.protos.HashGrpc

object ApiActor extends Role {

  val name = "api"

  def props(cfg: AppConfig, db: ActorRef): Props =
    Props(new ApiActor(cfg, db))
}

class ApiActor(cfg: AppConfig, db: ActorRef) extends BaseActor {

  val server: Server = initServer()
  def initServer(): Server = {
    val service = GrpcHashService(db, cfg.api)
    val serviceDefinition = HashGrpc.bindService(service, context.dispatcher)
    val contextInterceptor = new ContextServerInterceptor(log)

    val nettyServerBuilder =
      ServerBuilder
        .forPort(cfg.api.port)
        .addService(ServerInterceptors.intercept(serviceDefinition, List(contextInterceptor).asJava))
        .build()

    log.info("starting grpc service at port {}", 50050)
    nettyServerBuilder.start()
  }

  override def preStart(): Unit =
    log.info("api starting up ...")

  override def postStop(): Unit =
    server.shutdown()

  override def receive: Receive = Actor.emptyBehavior

}
