package com.ambantis.akmeter

import scala.collection.JavaConverters._

import akka.actor.{ActorRef, Props}
import com.typesafe.config.{Config, ConfigException}

/** Represents a High Level grouping of application code within the Application.
 *
 * Roles represent children of the application top level actor. Generally, there
 * are two categories of children:
 *   1. shared resources (e.g. DbActor)
 *   2. application logic
 *
 * Use of roles enables the application to define which children the topLevel actor will
 * create. For example, if the configuration specifies just the API role, then the InputActor
 * will not be instantiated.
 *
 * This trait should be extended by companion object of application-logic actors that will be
 * children of the TopLevelActor.
 */
trait Role {
  // the name of the actor
  def name: String
  // the props of the actor
  def props(cfg: AppConfig, db: ActorRef): Props
}

object Role {

  def show(roles: Iterable[Role]): String = roles.map(_.name).mkString("[", ", ", "]")

  @throws[IllegalArgumentException]("unknown role")
  def apply(name: String): Role = name.toLowerCase match {
    case unk => throw new IllegalArgumentException(s"unknown role $unk")
  }

  @throws[ConfigException]("role not defined")
  @throws[IllegalArgumentException]("sim role is incompatible with other roles")
  @throws[IllegalArgumentException]("unknown role")
  def fromConfig(config: Config, basePath: String): Set[Role] = {
    val roles: Set[Role] =
      config
        .getStringList(s"$basePath.roles")
        .asScala
        .map(name => Role(name))
        .toSet

    if (roles.isEmpty) All
    else roles

  }

  // all application roles (excludes performance testing role)
  final val All: Set[Role] = Set(api.ApiActor)
}
