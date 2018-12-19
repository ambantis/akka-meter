import sbt._

object Dependencies {

  object Version {

    lazy val akkaCore = "2.5.19"
    lazy val scalaTest = "3.0.5"
  }

  lazy val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % Version.akkaCore,
    "com.typesafe.akka" %% "akka-stream" % Version.akkaCore
  )

  lazy val testing = Seq(
    "org.scalatest" %% "scalatest" % Version.scalaTest,
    "com.typesafe.akka" %% "akka-testkit" % Version.akkaCore,
    "com.typesafe.akka" %% "akka-stream-testkit" % Version.akkaCore
  ).map(_ % Test)

  lazy val all = akka ++ testing
}
