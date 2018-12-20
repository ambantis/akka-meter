package com.ambantis.akmeter

import com.typesafe.config.Config
import com.ambantis.akmeter.db.DbConfig

final case class AppConfig(roles: Set[Role], db: DbConfig)

object AppConfig {

  val root = "akmeter"

  def apply(config: Config): AppConfig =
    AppConfig(
      roles = Role.fromConfig(config, root),
      db = DbConfig(config, root)
    )
}
