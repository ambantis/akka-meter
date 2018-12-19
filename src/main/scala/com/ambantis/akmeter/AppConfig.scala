package com.ambantis.akmeter

import com.typesafe.config.Config

final case class AppConfig(roles: Set[Role])

object AppConfig {

  def apply(config: Config): AppConfig =
    AppConfig(Role.fromConfig(config, "akmeter"))
}
