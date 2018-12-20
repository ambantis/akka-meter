import sbt._

object Dependencies {

  object Version {

    lazy val akkaCore = "2.5.19"
    lazy val scalaTest = "3.0.5"
    lazy val logback = "1.2.3"
    lazy val scalaPB = scalapb.compiler.Version.scalapbVersion
    lazy val scalaRuntimePB = scalapb.compiler.Version.scalapbVersion
    lazy val grpcJava = scalapb.compiler.Version.grpcJavaVersion
  }

  lazy val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % Version.akkaCore,
    "com.typesafe.akka" %% "akka-slf4j" % Version.akkaCore,
    "com.typesafe.akka" %% "akka-stream" % Version.akkaCore
  )

  lazy val grpc = Seq(
    "com.thesamet.scalapb" %% "scalapb-runtime" % Version.scalaPB % "protobuf",
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % Version.scalaRuntimePB,
    "io.grpc" % "grpc-netty" % Version.grpcJava
  )

  lazy val misc = Seq(
    "ch.qos.logback" % "logback-classic" % Version.logback
  )

  lazy val testing = Seq(
    "org.scalatest" %% "scalatest" % Version.scalaTest,
    "com.typesafe.akka" %% "akka-testkit" % Version.akkaCore,
    "com.typesafe.akka" %% "akka-stream-testkit" % Version.akkaCore
  ).map(_ % Test)

  lazy val all = akka ++ grpc ++ misc ++ testing
}
