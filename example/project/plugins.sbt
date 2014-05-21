// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.0-RC1")

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.0.0-RC2")

addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.0.0-SNAPSHOT")

addSbtPlugin("me.dribba.sbt" % "sbt-es6-module-transpiler" % "1.0.0-SNAPSHOT")

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0-RC1")