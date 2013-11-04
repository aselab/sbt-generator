package com.github.aselab.sbt

import org.specs2.mutable._
import scala.Console._

object StatusSpec extends Specification {
  "Status" should {
    val status = Status.Create

    "color format" in {
      status.format(true) mustEqual status.color + "    create" + RESET
    }

    "no color format" in {
      status.format(false) mustEqual "    create"
    }
  }
}
