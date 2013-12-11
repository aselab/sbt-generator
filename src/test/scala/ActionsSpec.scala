package com.github.aselab.sbt

import sbt._
import org.specs2.mutable._
import org.specs2.mock._
import org.specs2.specification.BeforeAfterExample
import Status._
import scala.Console._

object ActionsSpec extends Specification with BeforeAfterExample with Mockito {
  sequential

  class Tester(var mode: Mode.Value = Mode.Invoke) extends RevokableActions {
    val logger = mock[GeneratorLogger]
    val scalateTemplate = mock[ScalateTemplate]
  }

  class MockIO(input: String) extends BeforeAfter {
    lazy val mockIn = new java.io.ByteArrayInputStream(input.getBytes)
    lazy val mockOut = new java.io.ByteArrayOutputStream()

    def before = {
      setIn(mockIn)
      setOut(mockOut)
    }

    def after = {
      setIn(in)
      setOut(out)
    }
  }

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
    val actions = new Tester
    "createDirectory" in {
      val dir = workDir / "test"
      actions.createDirectory(dir) mustEqual Create
      there was one(actions.logger).log(Create, dir)
      dir.exists must beTrue

      actions.createDirectory(dir) mustEqual Exist
      there was one(actions.logger).log(Exist, dir)
    }

    "removeDirectory" in {
      val dir = workDir / "test"
      IO.createDirectory(dir)
      actions.removeDirectory(dir) mustEqual Remove
      there was one(actions.logger).log(Remove, dir)
      dir.exists must beFalse

      actions.removeDirectory(dir) mustEqual Skip
      there was one(actions.logger).log(Skip, dir)
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

    "createFile with conflict" in new MockIO("y") {
      val oldContent = "line1\nline2\nline3"
      val newContent = "changed"
      val file = createTestFile(oldContent)
      
      actions.createFile(file, newContent) mustEqual Force
      IO.read(file) mustEqual newContent
    }

    "removeFile" in {
      val file = workDir / "testFile"
      IO.touch(file)
      actions.removeFile(file) mustEqual Remove
      there was one(actions.logger).log(Remove, file)
      file.exists must beFalse

      actions.removeFile(file) mustEqual Skip
      there was one(actions.logger).log(Skip, file)
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

    "diff" in {
      val content = """
      |aaa
      |bbb
      |ccc
      |""".stripMargin

      val newContent = """
      |aaa
      |ccc
      |ddd
      |""".stripMargin

      val diff = """
      | aaa
      |-bbb
      | ccc
      |+ddd""".stripMargin

      val file = createTestFile(content)
      actions.diff(file, newContent, false) must contain(diff)

      val coloredDiff = actions.diff(file, newContent, true)
      coloredDiff must contain(RED + "-bbb" + RESET)
      coloredDiff must contain(GREEN + "+ddd" + RESET)
    }

    "resolveConflict" in {
      val oldContent = """
      |aaa
      |bbb
      |ccc
      |""".stripMargin

      val newContent = """
      |aaa
      |ccc
      |ddd
      |""".stripMargin

      "answers yes" in new MockIO("y") {
        val file = createTestFile(oldContent)
        actions.resolveConflict(file, newContent) mustEqual Force
        there was one(actions.logger).log(Force, file)
        IO.read(file) mustEqual newContent
      }

      "answers no" in new MockIO("n") {
        val file = createTestFile(oldContent)
        actions.resolveConflict(file, newContent) mustEqual Skip
        there was one(actions.logger).log(Skip, file)
        IO.read(file) mustEqual oldContent
      }

      "answers help, diff, yes" in new MockIO("h\nd\ny") {
        val file = createTestFile(oldContent)
        actions.resolveConflict(file, newContent) mustEqual Force
        mockOut.toString must contain("show this help")
        mockOut.toString must contain("--- " + file.getPath)
      }
    }
  }

  "TemplateActions" should {
    val actions = new Tester
    "template" in {
      val dst = workDir / "test.txt"
      val src = "test.txt.ssp"
      actions.scalateTemplate.render(src, Map("name" -> "John")) returns "Hello John"
      actions.template(dst, src, "name" -> "John") mustEqual Create
      IO.read(dst) mustEqual "Hello John"

      actions.template(dst, src, "name" -> "John") mustEqual Identical
    }
  }

  "RevokableActions" should {
    val actions = new Tester(Mode.Revoke)

    "createDirectory" in {
      val dir = workDir / "test"
      IO.createDirectory(dir)
      actions.createDirectory(dir) mustEqual Remove
      there was one(actions.logger).log(Remove, dir)
      dir.exists must beFalse

      actions.createDirectory(dir) mustEqual Skip
      there was one(actions.logger).log(Skip, dir)
    }

    "copyDirectory" in {
      val files = Seq(
        workDir / "dir1",
        workDir / "dir1/dir2",
        workDir / "dir1/dir2/dir3",
        workDir / "dir1/dir2/file1",
        workDir / "dir1/file2",
        workDir / "dir4",
        workDir / "dir4/file3"
      )
      val additionalFile = workDir / "dir1/file4"
      IO.copyDirectory(copyDir, workDir)
      IO.touch(additionalFile)

      actions.copyDirectory(copyDir, workDir) must containTheSameElementsAs(files)

      files.tail must contain((f: File) => !f.exists).foreach
      files.head.exists must beTrue
      additionalFile.exists must beTrue
    }

    "createFile" in {
      val file = workDir / "testFile"
      IO.touch(file)
      actions.createFile(file, "") mustEqual Remove
      there was one(actions.logger).log(Remove, file)
      file.exists must beFalse

      actions.createFile(file, "") mustEqual Skip
      there was one(actions.logger).log(Skip, file)
    }

    "copyFile" in {
      val src = copyDir / "dir4/file3"
      val dst = workDir / "dir4/file3"
      IO.copyFile(src, dst)
      actions.copyFile(src, dst) mustEqual Remove
      there was one(actions.logger).log(Remove, dst)
      dst.exists must beFalse

      actions.copyFile(src, dst) mustEqual Skip
      there was one(actions.logger).log(Skip, dst)
    }

    "template" in {
      val dst = workDir / "test.txt"
      val src = "test.txt.ssp"
      IO.touch(dst)
      actions.template(dst, src, "name" -> "John") mustEqual Remove
      dst.exists must beFalse

      actions.template(dst, src, "name" -> "John") mustEqual Skip
    }

    "prependToFile" in {
      val content = "line1\nline2\n"
      val file = createTestFile("prepend" + content)
      actions.prependToFile(file, "prepend") mustEqual Subtract
      there was one(actions.logger).log(Subtract, file)
      IO.read(file) mustEqual content

      actions.prependToFile(file, "prepend") mustEqual Skip
      there was one(actions.logger).log(Skip, file)
    }

    "appendToFile" in {
      val content = "line1\nline2\n"
      val file = createTestFile(content + "append")
      actions.appendToFile(file, "append") mustEqual Subtract
      there was one(actions.logger).log(Subtract, file)
      IO.read(file) mustEqual content

      actions.appendToFile(file, "append") mustEqual Skip
      there was one(actions.logger).log(Skip, file)
    }

    "insertIntoFileBefore" in {
      val content = "line1\nline2\n"
      val file = createTestFile("line1\ninsert\nline2\n")
      actions.insertIntoFileBefore(file, "line2", "insert\n") mustEqual Subtract
      there was one(actions.logger).log(Subtract, file)
      IO.read(file) mustEqual "line1\nline2\n"

      actions.insertIntoFileBefore(file, "line2", "insert\n") mustEqual Skip
      there was one(actions.logger).log(Skip, file)
    }

    "insertIntoFileAfter" in {
      val content = "line1\nline2\n"
      val file = createTestFile("line1\ninsert\nline2\n")
      actions.insertIntoFileAfter(file, "line1\n", "insert\n") mustEqual Subtract
      there was one(actions.logger).log(Subtract, file)
      IO.read(file) mustEqual "line1\nline2\n"

      actions.insertIntoFileAfter(file, "line1\n", "insert\n") mustEqual Skip
      there was one(actions.logger).log(Skip, file)
      IO.read(file) mustEqual "line1\nline2\n"
    }

    "recordInvocations" in {
      val dir = workDir / "test"
      val file = dir / "file"
      IO.write(file, "test")
      val functions = actions.recordInvocations {
        actions.createDirectory(dir)
        actions.createFile(file, "test")
      }
      functions.size mustEqual 2
      file.exists must beTrue
      functions.reverse.foreach(_.apply)
      file.exists must beFalse
    }
  }
}
