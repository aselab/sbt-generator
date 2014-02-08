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

  def get(generators: Seq[Generator[_]], name: String): Generator[Any] = {
    generators.find(_.name == name).getOrElse(
      sys.error(name + " generator is not found")
    ).asInstanceOf[Generator[Any]]
  }

  def loader(generators: Seq[Generator[_]]) = {
    val urls = generators.reverse.map(_.location).distinct.toArray
    new java.net.URLClassLoader(urls)
  }

  def generate = Def.inputTask {
    parser.parsed match {
      case (name: String, args) =>
        val all = generators.value
        val context = GeneratorContext(state.value, streams.value.log, loader(all))
        get(all, name).invoke(args)(context)
    }
  }

  def destroy = Def.inputTask {
    parser.parsed match {
      case (name: String, args) =>
        val all = generators.value
        val context = GeneratorContext(state.value, streams.value.log, loader(all))
        get(all,name).revoke(args)(context)
    }
  }
}
