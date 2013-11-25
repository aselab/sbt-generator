package com.github.aselab.sbt

import sbt._

object Keys {
  lazy val generators = SettingKey[Seq[Generator[_]]]("generators")
  lazy val generate = InputKey[Unit]("generate")
  lazy val destroy = InputKey[Unit]("destroy")
  lazy val copyTemplates = TaskKey[Unit]("copyTemplates")
  lazy val templateDirectory = SettingKey[java.io.File]("templateDirectory")
}

