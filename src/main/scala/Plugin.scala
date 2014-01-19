package com.github.aselab.sbt

import sbt._
import sbt.Keys._
import Keys._
import sbt.complete.DefaultParsers._

object Plugin extends sbt.Plugin {
  val GeneratorKeys = Keys

  val generatorSettings = Seq(
    generators := Nil,
    generate <<= GeneratorTasks.generate,
    destroy <<= GeneratorTasks.destroy,
    copyTemplates <<= GeneratorTasks.copyTemplates,
    templateDirectory := baseDirectory.value / "templates"
  )
}

object GeneratorTasks {
  def parser = Def.setting {
    if (generators.value.size > 0) {
      generators.value.asInstanceOf[Seq[Generator[Any]]].map { g =>
        val parser = Space ~> (token(g.name) ~ g.argumentsParser)
        parser !!! "Usage: generate %s %s".format(g.name, g.help)
      }.reduceLeft {(a, b) => a | b}
    } else {
      failure("No generator is registered")
    }
  }

  def copyTemplates = Def.task {
    val dir = templateDirectory.value
    val actions = new TaskActions(getClass.getClassLoader, streams.value.log)
    actions.copyResources("templates", dir)
  }

  def generate = Def.inputTask {
    parser.parsed match {
      case (name: String, args) =>
        val context = GeneratorContext(state.value, streams.value.log)
        val generator = generators.value.find(_.name == name).getOrElse(
          sys.error(name + " generator is not found")
        )
        generator.asInstanceOf[Generator[Any]].invoke(args)(context)
    }
  }

  def destroy = Def.inputTask {
    parser.parsed match {
      case (name: String, args) =>
        val context = GeneratorContext(state.value, streams.value.log)
        val generator = generators.value.find(_.name == name).getOrElse(
          sys.error(name + " generator is not found")
        )
        generator.asInstanceOf[Generator[Any]].revoke(args)(context)
    }
  }
}
