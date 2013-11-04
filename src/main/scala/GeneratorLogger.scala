package com.github.aselab.sbt

import sbt._

class GeneratorLogger(underlying: Logger) extends Logger {
  def trace(t: => Throwable): Unit = underlying.trace(t)
  def success(message: => String): Unit = underlying.success(message)
  def log(level: Level.Value, message: => String): Unit =
    underlying.log(level, message)

  override def ansiCodesSupported: Boolean = underlying.ansiCodesSupported

  def log(status: Status, message: => String): Unit =
    info(status.format(ansiCodesSupported) + "  " + message)

  def log(status: Status, file: File): Unit = log(status, file.toString)
}

