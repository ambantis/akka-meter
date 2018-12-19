
lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.ambantis",
      scalaVersion := "2.12.7",
      fork := true
    )),
    name := "akka-meter",
    libraryDependencies ++= Dependencies.all
  )
