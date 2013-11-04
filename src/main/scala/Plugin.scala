package com.github.aselab.sbt

import sbt._
import sbt.Keys._
import Keys._

object Plugin extends sbt.Plugin {
  val generatorSettings = Seq(
    generate <<= Task.generate,
    copyTemplates <<= Task.copyTemplates,
    templateDirectory := baseDirectory.value / "templates"
  )

  object Task {
    def copyTemplates = Def.task {
      val dir = templateDirectory.value
      val actions = new TaskActions(getClass.getClassLoader, streams.value.log)
      actions.copyResources("templates", dir)
    }

    def generate = Def.inputTask { Generator.parser.parsed match {
      case (name: String, args) =>
        val context = new GeneratorContext(state.value, streams.value.log)
        Generator(name).asInstanceOf[Generator[Any]].invoke(args)(context)
    }}
  }
}
