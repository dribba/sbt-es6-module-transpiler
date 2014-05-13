sbtPlugin := true

organization := "me.dribba.sbt"

name := "sbt-es6-module-transpiler"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
)

addSbtPlugin("com.typesafe.sbt" %% "sbt-js-engine" % "1.0.0-SNAPSHOT")

publishMavenStyle := false

publishTo := {
  if (isSnapshot.value) Some(Classpaths.sbtPluginSnapshots)
  else Some(Classpaths.sbtPluginReleases)
}

scriptedSettings

scriptedLaunchOpts <+= version apply { v => s"-Dproject.version=$v" }

