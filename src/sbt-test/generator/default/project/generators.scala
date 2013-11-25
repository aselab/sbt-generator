package sample

import com.github.aselab.sbt._
import sbt._
import sbt.Keys._

object Sample1Generator extends NoArgGenerator("sample1") {
  def generate: Unit = {
    val file = context(baseDirectory) / "sample1" / "file"
    createFile(file, "sample1")
  }
}

object Sample2Generator extends DefaultGenerator("sample2") {
  def generate(args: Seq[String]): Unit = {
    val file = context(baseDirectory) / "sample2" / "file"
    template(file, "sample2.ssp", "name" -> args.headOption.getOrElse(""))
  }
}
