name := "flow-helper"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.6"

enablePlugins(JavaAppPackaging)

resolvers ++= Seq(
  "Maven Central Server"          at "http://repo1.maven.org/maven2",
  "TypeSafe Repository Releases"  at "http://repo.typesafe.com/typesafe/releases/",
  "TypeSafe Repository Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
)

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  "io.argonaut"   %% "argonaut"   % "6.1",
  "org.scalatest" %% "scalatest"  % "3.0.1" % "test"
)


