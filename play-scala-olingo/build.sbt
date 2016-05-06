name := "play-scala-olingo"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

routesGenerator := InjectedRoutesGenerator

val oDataVersion = "4.2.0"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.apache.olingo" % "odata-server-api" % oDataVersion,
  "org.apache.olingo" % "odata-server-core" % oDataVersion,
  "org.apache.olingo" % "odata-commons-api" % oDataVersion,
  "org.apache.olingo" % "odata-commons-core" % oDataVersion
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
