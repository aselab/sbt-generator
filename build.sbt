name := "sbt-generator"

organization := "com.github.aselab"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.10.3"

sbtPlugin := true

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "org.fusesource.scalate" %% "scalate-core" % "1.6.1",
  "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0",
  "org.specs2" %% "specs2" % "2.2.2" % "test",
  "org.mockito" % "mockito-all" % "1.9.5" % "test"
)

scalacOptions ++= Seq("-deprecation", "-unchecked")

ScriptedPlugin.scriptedSettings

ScriptedPlugin.scriptedBufferLog := false

ScriptedPlugin.scriptedLaunchOpts += "-Dversion=" + version.value

watchSources ++= ScriptedPlugin.sbtTestDirectory.value.***.get
