package com.github.aselab.sbt

import scala.Console._

sealed abstract class Status(val color:String) extends Throwable with Product {
  def format(colored: Boolean): String = {
    val s = "%10s".format(productPrefix.toLowerCase)
    if (colored) color + s + RESET else s
  }
}

object Status {
  case object Invoke extends Status(BOLD)
  case object Revoke extends Status(BOLD)
  case object Record extends Status(BOLD)
  case object Create extends Status(GREEN)
  case object Prepend extends Status(GREEN)
  case object Append extends Status(GREEN)
  case object Insert extends Status(GREEN)
  case object Skip extends Status(YELLOW)
  case object Force extends Status(YELLOW)
  case object Exist extends Status(CYAN)
  case object Identical extends Status(CYAN)
  case object Conflict extends Status(RED)
  case object Remove extends Status(RED)
  case object Subtract extends Status(RED)
  case object Abort extends Status(RED)
}
