package com.github.aselab.sbt

import sbt._
import sbt.Keys._
import Keys._

object Plugin extends sbt.Plugin {
  val generatorSettings = Seq(
    generators := Nil,
    generate <<= Task.generate,
    destroy <<= Task.destroy,
    copyTemplates <<= Task.copyTemplates,
    templateDirectory := baseDirectory.value / "templates"
  )

  object Task {
    lazy val parser = Def.setting {
      generators.value.foreach(_.register)
      Generator.parser
    }

    def copyTemplates = Def.task {
      val dir = templateDirectory.value
      val actions = new TaskActions(getClass.getClassLoader, streams.value.log)
      actions.copyResources("templates", dir)
    }

    def generate = Def.inputTask {
      parser.parsed match {
        case (name: String, args) =>
          val context = new GeneratorContext(state.value, streams.value.log)
          Generator(name).asInstanceOf[Generator[Any]].invoke(args)(context)
      }
    }

    def destroy = Def.inputTask {
      parser.parsed match {
        case (name: String, args) =>
          val context = new GeneratorContext(state.value, streams.value.log)
          Generator(name).asInstanceOf[Generator[Any]].revoke(args)(context)
      }
    }
  }
}
