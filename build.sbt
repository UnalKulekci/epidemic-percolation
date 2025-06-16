ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.6"

lazy val root = (project in file("."))
  .settings(
    name := "epidemic",
    
  )

  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.10" % Test
  libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4"
