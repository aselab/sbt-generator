package com.github.aselab.sbt

import sbt._
import sbt.Keys._
import Keys._
import sbt.complete.Parser
import sbt.complete.DefaultParsers._
import scala.util.DynamicVariable

object Mode extends Enumeration {
  val Invoke, Revoke = Value
}

class GeneratorContext(
  state: State, streamLogger: Logger
) {
  val extracted: Extracted = Project.extract(state)
  def apply[T](key: SettingKey[T]): T = extracted.get(key)
  def apply[T](key: TaskKey[T]): T = extracted.runTask(key, state)._2

  val logger = new GeneratorLogger(streamLogger)
  lazy val scalaJar: File = apply(scalaInstance).libraryJar
  lazy val templateDir: File = apply(templateDirectory)
  lazy val sourceDir: File = apply(scalaSource in Compile)
  lazy val scalateTemplate = new ScalateTemplate(scalaJar, templateDir)
}

trait Generator[ArgumentsType] extends RevokableActions {
  def name: String
  def help: String
  def argumentsParser: Parser[ArgumentsType]

  private var _mode: Mode.Value = _
  def mode = _mode
  protected def generate(args: ArgumentsType): Unit
  protected def destroy(args: ArgumentsType): Unit = {
    val functions = recordInvocations { generate(args) }
    functions.reverse.foreach(_.apply)
  }

  def invoke(args: ArgumentsType)(implicit context: GeneratorContext) =
    _context.withValue(context) {
      _mode = Mode.Invoke
      generate(args.asInstanceOf[ArgumentsType])
    }

  def revoke(args: ArgumentsType)(implicit context: GeneratorContext) =
    _context.withValue(context) {
      _mode = Mode.Revoke
      destroy(args.asInstanceOf[ArgumentsType])
    }

  def register = Generator.register(this)

  private val _context = new DynamicVariable[GeneratorContext](null)
  def scalateTemplate = context.scalateTemplate
  def logger = context.logger
  def sourceDir = context.sourceDir
  implicit protected def context = _context.value
}

abstract class DefaultGenerator(val name: String) extends Generator[Seq[String]] {
  def help = "[args]*"
  def argumentsParser: Parser[Seq[String]] = spaceDelimited("args*")
}

abstract class NoArgGenerator(val name: String) extends Generator[Unit] {
  def help = ""
  def argumentsParser: Parser[Unit] = EOF
}

object Generator {
  private val generators = collection.mutable.Map[String, Generator[_]]()
  private val parsers = collection.mutable.MutableList[Parser[_]]()

  def parser = if (parsers.size > 0) {
    parsers.reduceLeft {(a, b) => a | b}
  } else {
    failure("No generator is registered")
  }

  def apply(name: String): Generator[_] = generators(name)

  def register(generator: Generator[_]) {
    import generator._
    generators += (name -> generator)
    val parser = Space ~> (token(name) ~ argumentsParser)
    parsers += parser !!! "Usage: generate %s %s".format(name, help)
  }
}
