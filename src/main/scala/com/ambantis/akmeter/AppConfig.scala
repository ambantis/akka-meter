package com.ambantis.akmeter

import com.typesafe.config.Config

final case class AppConfig()

object AppConfig {

  def apply(config: Config): AppConfig = AppConfig()

}
