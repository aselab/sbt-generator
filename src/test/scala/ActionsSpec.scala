package com.github.aselab.sbt

import sbt._
import org.specs2.mutable._
import org.specs2.mock._
import org.specs2.specification._
import Status._

object ActionsSpec extends Specification with BeforeAfterExample with Mockito {
  sequential

  class Tester extends FileActions with TemplateActions {
    val logger = mock[GeneratorLogger]
    val scalateTemplate = mock[ScalateTemplate]
  }

  val actions = new Tester
  var workDir: File = _
  lazy val copyDir = file(getClass.getResource("/root").getPath)

  def before {
    workDir = IO.createTemporaryDirectory
  }

  def after {
    IO.delete(workDir)
  }

  def createTestFile(data: String) = {
    val file = workDir / "testFile"
    IO.write(file, data)
    file
  }

  "FileActions" should {
    "createDirectory" in {
      val dir = workDir / "test"
      actions.createDirectory(dir) mustEqual Create
      there was one(actions.logger).log(Create, dir)
      dir.exists must beTrue

      actions.createDirectory(dir) mustEqual Exist
      there was one(actions.logger).log(Exist, dir)
    }

    "copyDirectory" in {
      actions.copyDirectory(copyDir, workDir) must contain(
        workDir / "dir1",
        workDir / "dir1/dir2",
        workDir / "dir1/dir2/dir3",
        workDir / "dir1/dir2/file1",
        workDir / "dir1/file2",
        workDir / "dir4",
        workDir / "dir4/file3"
      )

      Seq("dir1/dir2/file1", "dir1/file2", "dir4/file3") must contain(
        (path: String) =>
          IO.read(copyDir / path) mustEqual IO.read(workDir/ path)
      ).foreach
    }

    "createFile" in {
      val content = "line1\nline2\n"
      val file = workDir / "testFile"
      actions.createFile(file, content) mustEqual Create
      there was one(actions.logger).log(Create, file)
      IO.read(file) mustEqual content

      actions.createFile(file, content) mustEqual Identical
      there was one(actions.logger).log(Identical, file)
      IO.read(file) mustEqual content
    }

    "copyFile" in {
      val src = copyDir / "dir4/file3"
      val dst = workDir / "dir4/file3"
      actions.copyFile(src, dst) mustEqual Create
      there was one(actions.logger).log(Create, dst.getParentFile)
      there was one(actions.logger).log(Create, dst)
      IO.read(dst) mustEqual IO.read(src)

      actions.copyFile(src, dst) mustEqual Identical
      there was one(actions.logger).log(Identical, dst)
      IO.read(dst) mustEqual IO.read(src)
    }

    "prependToFile" in {
      val content = "line1\nline2\n"
      val file = createTestFile(content)
      actions.prependToFile(file, "prepend") mustEqual Prepend
      there was one(actions.logger).log(Prepend, file)
      IO.read(file) mustEqual "prepend" + content

      actions.prependToFile(file, "prepend") mustEqual Identical
      there was one(actions.logger).log(Identical, file)
      IO.read(file) mustEqual "prepend" + content
    }

    "appendToFile" in {
      val content = "line1\nline2\n"
      val file = createTestFile(content)
      actions.appendToFile(file, "append") mustEqual Status.Append
      there was one(actions.logger).log(Status.Append, file)
      IO.read(file) mustEqual content + "append"

      actions.appendToFile(file, "append") mustEqual Identical
      there was one(actions.logger).log(Identical, file)
      IO.read(file) mustEqual content + "append"
    }

    "insertIntoFileBefore" in {
      val content = "line1\nline2\n"
      val file = createTestFile(content)
      actions.insertIntoFileBefore(file, "line2", "insert\n") mustEqual Insert
      there was one(actions.logger).log(Insert, file)
      IO.read(file) mustEqual "line1\ninsert\nline2\n"

      actions.insertIntoFileBefore(file, "line2", "insert\n") mustEqual Identical
      there was one(actions.logger).log(Identical, file)
      IO.read(file) mustEqual "line1\ninsert\nline2\n"

      actions.insertIntoFileBefore(file, "not found pattern", "insert") mustEqual Skip
      there was one(actions.logger).log(Skip, file)
    }

    "insertIntoFileAfter" in {
      val content = "line1\nline2\n"
      val file = createTestFile(content)
      actions.insertIntoFileAfter(file, "line1\n", "insert\n") mustEqual Insert
      there was one(actions.logger).log(Insert, file)
      IO.read(file) mustEqual "line1\ninsert\nline2\n"

      actions.insertIntoFileAfter(file, "line1\n", "insert\n") mustEqual Identical
      there was one(actions.logger).log(Identical, file)
      IO.read(file) mustEqual "line1\ninsert\nline2\n"

      actions.insertIntoFileAfter(file, "not found pattern", "insert") mustEqual Skip
      there was one(actions.logger).log(Skip, file)
    }

    "insertIntoClass" in {
      val content = """
      |package com.github.aselab
      |
      |case class Sample(s: String) extends SuperClass with SomeTrait {
      |  val a = 1
      |}
      """.stripMargin

      val insert = "\n  val b = 2"

      val expect = """
      |package com.github.aselab
      |
      |case class Sample(s: String) extends SuperClass with SomeTrait {
      |  val b = 2
      |  val a = 1
      |}
      """.stripMargin

      val file = createTestFile(content)
      actions.insertIntoClass(file, "Sample", insert) mustEqual Insert
      IO.read(file) mustEqual expect

      actions.insertIntoClass(file, "NotExist", insert) mustEqual Skip
      IO.read(file) mustEqual expect
    }

    "insertIntoTrait" in {
      val content = """
      |package com.github.aselab
      |
      |trait Sample {
      |  val a = 1
      |}
      """.stripMargin

      val insert = "\n  val b = 2"

      val expect = """
      |package com.github.aselab
      |
      |trait Sample {
      |  val b = 2
      |  val a = 1
      |}
      """.stripMargin

      val file = createTestFile(content)
      actions.insertIntoTrait(file, "Sample", insert) mustEqual Insert
      IO.read(file) mustEqual expect
    }

    "insertIntoObject" in {
      val content = """
      |package com.github.aselab
      |
      |object Sample extends SuperClass with SomeTrait
      |{
      |  val a = 1
      |}
      """.stripMargin

      val insert = "\n  val b = 2"

      val expect = """
      |package com.github.aselab
      |
      |object Sample extends SuperClass with SomeTrait
      |{
      |  val b = 2
      |  val a = 1
      |}
      """.stripMargin

      val file = createTestFile(content)
      actions.insertIntoObject(file, "Sample", insert) mustEqual Insert
      IO.read(file) mustEqual expect
    }
  }

  "TemplateActions" should {
    "template" in {
      val dst = workDir / "test.txt"
      val src = "test.txt.ssp"
      actions.scalateTemplate.render(src, Map("name" -> "John")) returns "Hello John"
      actions.template(dst, src, "name" -> "John") mustEqual Create
      IO.read(dst) mustEqual "Hello John"

      actions.template(dst, src, "name" -> "John") mustEqual Identical
    }
  }
}
