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

case class GeneratorContext(
  state: State, streamLogger: Logger, indentLevel: Int = 0
) {
  lazy val extracted: Extracted = Project.extract(state)
  def apply[T](key: SettingKey[T]): T = extracted.get(key)
  def apply[T](key: TaskKey[T]): T = extracted.runTask(key, state)._2

  lazy val logger = new GeneratorLogger(streamLogger, "  " * indentLevel)
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

  def invoke(args: ArgumentsType)(implicit context: GeneratorContext) = {
    context.logger.log(Status.Invoke, name)
    _context.withValue(context.copy(indentLevel = context.indentLevel + 1)) {
      _mode = Mode.Invoke
      try {
        overwriteAll = false
        generate(args.asInstanceOf[ArgumentsType])
      } catch {
        case Status.Abort => logger.log(Status.Abort)
      }
    }
  }

  def revoke(args: ArgumentsType)(implicit context: GeneratorContext) = {
    context.logger.log(Status.Revoke, name)
    _context.withValue(context.copy(indentLevel = context.indentLevel + 1)) {
      _mode = Mode.Revoke
      try {
        overwriteAll = false
        destroy(args.asInstanceOf[ArgumentsType])
      } catch {
        case Status.Abort => logger.log(Status.Abort)
      }
    }
  }

  private val _context = new DynamicVariable[GeneratorContext](null)
  def scalateTemplate = context.scalateTemplate
  def logger = context.logger
  def sourceDir = context.sourceDir
  implicit protected def context = _context.value

  private var overwriteAll = false
  override def resolveConflict(file: File, data: String) = if (overwriteAll) {
    ConflictResolver.yes.resolve(file, data)
  } else {
    super.resolveConflict(file, data)
  }

  ConflictResolver.add("all", "overwrite this and all others") { (file, data) =>
    overwriteAll = true
    ConflictResolver.yes.resolve(file, data)
  }

  ConflictResolver.add("quit", "abort") { (file, data) =>
    throw Status.Abort
  }
}

abstract class DefaultGenerator(val name: String) extends Generator[Seq[String]] {
  def help = "[args]*"
  def argumentsParser: Parser[Seq[String]] = spaceDelimited("args*")
}

abstract class NoArgGenerator(val name: String) extends Generator[Unit] {
  def help = ""
  def argumentsParser: Parser[Unit] = EOF

  protected def generate: Unit
  protected def destroy: Unit = super.destroy()
  protected def generate(args: Unit): Unit = generate
  override protected def destroy(args: Unit): Unit = destroy

  def invoke(implicit context: GeneratorContext) = super.invoke()
  override def invoke(args: Unit)(implicit context: GeneratorContext) = invoke

  def revoke(implicit context: GeneratorContext) = super.revoke()
  override def revoke(args: Unit)(implicit context: GeneratorContext) = revoke
}

