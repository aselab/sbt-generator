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

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

ScriptedPlugin.scriptedSettings

ScriptedPlugin.scriptedBufferLog := false

ScriptedPlugin.scriptedLaunchOpts += "-Dversion=" + version.value

watchSources ++= ScriptedPlugin.sbtTestDirectory.value.***.get

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra :=
  <url>https://github.com/aselab/sbt-generator</url>
  <licenses>
    <license>
      <name>MIT License</name>
      <url>http://www.opensource.org/licenses/mit-license.php</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>https://github.com/aselab/sbt-generator.git</url>
    <connection>scm:git:https://github.com/aselab/sbt-generator.git</connection>
  </scm>
  <developers>
    <developer>
      <id>a-ono</id>
      <name>Akihiro Ono</name>
      <url>https://github.com/a-ono</url>
    </developer>
    <developer>
      <id>y-yoshinoya</id>
      <name>Yuki Yoshinoya</name>
      <url>https://github.com/y-yoshinoya</url>
    </developer>
  </developers>
