organization := "karazinscalausersgroup"

name := "flow-helper"

version := "0.1.0"

scalaVersion := "2.12.0"

crossScalaVersions := Seq("2.12.0","2.11.8")

enablePlugins(JavaAppPackaging)

licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))

resolvers ++= Seq(
  "Maven Central Server"          at "http://repo1.maven.org/maven2",
  "TypeSafe Repository Releases"  at "http://repo.typesafe.com/typesafe/releases/",
  "TypeSafe Repository Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
)

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  "io.argonaut"   %% "argonaut"   % "6.2",
  "org.scalatest" %% "scalatest"  % "3.0.1" % "test"
)


