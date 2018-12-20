package com.ambantis.akmeter

import com.typesafe.config.Config
import com.ambantis.akmeter.api.ApiConfig
import com.ambantis.akmeter.db.DbConfig

final case class AppConfig(roles: Set[Role], api: ApiConfig, db: DbConfig)

object AppConfig {

  val root = "akmeter"

  def apply(config: Config): AppConfig =
    AppConfig(
      roles = Role.fromConfig(config, root),
      api = ApiConfig(config, root),
      db = DbConfig(config, root)
    )
}
