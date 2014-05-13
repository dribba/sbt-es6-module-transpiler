name := """es6-example"""

//scalaVersion := "2.11.0"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.3.0-RC1",
  "org.webjars" % "bootstrap" % "2.3.1",
  "org.webjars" % "requirejs" % "2.1.11-1"
)


lazy val root = (project in file(".")).enablePlugins(play.PlayScala)

SbtCoffeeScript.autoImport.CoffeeScriptKeys.bare := true

unmanagedSourceDirectories in(Assets, Es6ModulesKeys.es6modules) := (unmanagedSourceDirectories in Assets).value :+ (resourceManaged in(Assets, CoffeeScriptKeys.coffeescript)).value
