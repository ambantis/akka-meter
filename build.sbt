import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.ambantis",
      scalaVersion := "2.12.7"
    )),
    name := "akka-meter",
    libraryDependencies ++= Dependencies.all
  )
