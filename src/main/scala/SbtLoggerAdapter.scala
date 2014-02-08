package com.github.aselab.sbt.slf4j

import sbt.Level.{Error, Warn, Info, Debug}
import org.slf4j._
import org.slf4j.helpers.{MarkerIgnoringBase, MessageFormatter}

class SbtLoggerAdapter extends MarkerIgnoringBase {
  val logger = Option(SbtLoggerAdapter.logger)

  val isErrorEnabled = true
  val isWarnEnabled = true
  val isInfoEnabled = true
  val isDebugEnabled = false
  val isTraceEnabled = false

  def log(level: sbt.Level.Value, msg: => String) = logger.foreach { l =>
    level match {
      case Error if isErrorEnabled => l.error(msg)
      case Warn if isWarnEnabled => l.warn(msg)
      case Info if isInfoEnabled => l.info(msg)
      case Debug if isDebugEnabled => l.debug(msg)
    }
  }

  def error(msg: String) = log(Error, msg)
  def error(msg: String, t: Throwable) = log(Error, format(msg, t))
  def error(msg: String, arg1: Any) = log(Error, format(msg, arg1))
  def error(msg: String, arg1: Any, arg2: Any) = log(Error, format(msg, arg1, arg2))
  def error(msg: String, args: Array[Object]) = log(Error, format(msg, args))

  def warn(msg: String) = log(Warn, msg)
  def warn(msg: String, t: Throwable) = log(Warn, format(msg, t))
  def warn(msg: String, arg1: Any) = log(Warn, format(msg, arg1))
  def warn(msg: String, arg1: Any, arg2: Any) = log(Warn, format(msg, arg1, arg2))
  def warn(msg: String, args: Array[Object]) = log(Warn, format(msg, args))

  def info(msg: String) = log(Info, msg)
  def info(msg: String, t: Throwable) = log(Info, format(msg, t))
  def info(msg: String, arg1: Any) = log(Info, format(msg, arg1))
  def info(msg: String, arg1: Any, arg2: Any) = log(Info, format(msg, arg1, arg2))
  def info(msg: String, args: Array[Object]) = log(Info, format(msg, args))

  def debug(msg: String) = log(Debug, msg)
  def debug(msg: String, t: Throwable) = log(Debug, format(msg, t))
  def debug(msg: String, arg1: Any) = log(Debug, format(msg, arg1))
  def debug(msg: String, arg1: Any, arg2: Any) = log(Debug, format(msg, arg1, arg2))
  def debug(msg: String, args: Array[Object]) = log(Debug, format(msg, args))

  def trace(msg: String) = {}
  def trace(msg: String, t: Throwable) = {}
  def trace(msg: String, arg1: Any) = {}
  def trace(msg: String, arg1: Any, arg2: Any) = {}
  def trace(msg: String, args: Array[Object]) = {}

  def format(msg: String, t: Throwable) =
    msg + ": " + t.getStackTraceString
  def format(msg: String, arg1: Any) =
    MessageFormatter.format(msg, arg1).getMessage
  def format(msg: String, arg1: Any, arg2: Any) =
    MessageFormatter.format(msg, arg1, arg2).getMessage
  def format(msg: String, args: Array[Object]) =
    MessageFormatter.format(msg, args).getMessage
}

object SbtLoggerAdapter {
  var logger: sbt.Logger = _
}

class LoggerFactory extends ILoggerFactory {
  def getLogger(name: String) = new SbtLoggerAdapter
}
