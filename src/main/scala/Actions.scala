package com.github.aselab.sbt

import sbt._
import collection.JavaConversions.enumerationAsScalaIterator
import scala.util.matching.Regex
import java.util.regex.Pattern

trait FileActions {
  def logger: GeneratorLogger

  def createDirectory(dir: File): Status = {
    val status = if (dir.isDirectory) {
      Status.Exist
    } else {
      IO.createDirectory(dir)
      Status.Create
    }
    log(status, dir)
  }

  def removeDirectory(dir: File): Status = {
    val status = if (dir.isDirectory) {
      IO.deleteIfEmpty(Set(dir))
      if (dir.isDirectory) Status.Skip else Status.Remove
    } else {
      Status.Skip
    }
    log(status, dir)
  }

  def copyDirectory(source: File, destination: File): Seq[File] = {
    val l = source.getPath.length
    source.***.get.tail.map { src =>
      val dst = destination / src.getPath.drop(l)
      if (src.isDirectory) {
        createDirectory(dst)
      } else {
        copyFile(src, dst)
      }
      dst
    }
  }

  def createFile(file: File, data: => String): Status = {
    val d = data
    val status = if (file.isFile) {
      if (IO.read(file) == d) Status.Identical else Status.Conflict
    } else {
      val dir = file.getParentFile
      if (!dir.isDirectory) createDirectory(dir)
      IO.write(file, d)
      Status.Create
    }
    log(status, file)

    if (status == Status.Conflict) resolveConflict(file, d) else status
  }

  def removeFile(file: File): Status = {
    val status = if (file.isFile) {
      IO.delete(file)
      Status.Remove
    } else {
      Status.Skip
    }
    log(status, file)
  }

  def copyFile(source: File, destination: File): Status =
    createFile(destination, IO.read(source))

  def prependToFile(file: File, data: => String): Status =
    insertIntoFileAfter(file, "\\A", data)

  def appendToFile(file: File, data: => String): Status =
    insertIntoFileBefore(file, "\\z", data) 

  def insertIntoFileBefore(file: File, regex: String, data: => String): Status =
    insert(file, regex, data, after = false)

  def insertIntoFileAfter(file: File, regex: String, data: => String): Status =
    insert(file, regex, data, after = true)
  
  def insertIntoClass(file: File, className: String, data: => String): Status =
    insertIntoSource(file, "class", className, data)

  def insertIntoTrait(file: File, traitName: String, data: => String): Status =
    insertIntoSource(file, "trait", traitName, data)

  def insertIntoObject(file: File, objectName: String, data: => String): Status=
    insertIntoSource(file, "object", objectName, data)

  protected def insertIntoSource(file: File, _type: String, name: String, data: => String): Status = {
    val regex = _type + "\\s+" + name + "[^\\{]*\\{"
    insertIntoFileAfter(file, regex, data)
  }

  protected def insert(file: File, regex: String, data: => String,
    after: Boolean): Status = {
    val d = data
    val content = IO.read(file)
    val (searchPattern, replacement) = if (after) {
      ((regex + Pattern.quote(d)).r, "$0" + Regex.quoteReplacement(data))
    } else {
      ((Pattern.quote(d) + regex).r, Regex.quoteReplacement(data) + "$0")
    }

    val status = if (searchPattern.findFirstIn(content).isEmpty) {
      val replaced = content.replaceFirst(regex, replacement)
      if (replaced != content) {
        IO.write(file, replaced)
        regex match {
          case "\\A" => Status.Prepend
          case "\\z" => Status.Append
          case _ => Status.Insert
        }
      } else {
        Status.Skip
      }
    } else {
      Status.Identical
    }

    log(status, file)
  }

  def resolveConflict(file: File, data: String): Status = {
    val question = "The file %s exists, do you want to overwrite it? (y/n): ".format(file.getPath)
    def ask: Status = {
      scala.Console.readLine(question).toLowerCase.headOption match {
        case Some('y') =>
          IO.write(file, data)
          log(Status.Force, file)
        case Some('n') =>
          log(Status.Skip, file)
        case _ => ask
      }
    }
    ask
  }

  def log(status: Status, file: File): Status = {
    logger.log(status, file)
    status
  }
}

trait TemplateActions extends FileActions {
  def scalateTemplate: ScalateTemplate

  def template(destination: File, resource: String, args: Map[String, Any]): Status =
    createFile(destination, scalateTemplate.render(resource, args))

  def template(destination: File, resource: String, args: (String, Any)*): Status =
    template(destination, resource, args.toMap)
}

class TaskActions(loader: ClassLoader, _logger: Logger) extends FileActions {
  val logger = new GeneratorLogger(_logger)
  val mode = Mode.Invoke

  def copyResources(name: String, dir: File): Unit = {
    val created = collection.mutable.Map[File, Boolean]()
    loader.getResources(name).foreach(url =>
      url.getProtocol match {
        case "file" =>
          copyDirectory(file(url.getPath), dir).foreach {f => created(f) = true}
        case "jar" =>
          val con = url.openConnection.asInstanceOf[java.net.JarURLConnection]
          val entryName = con.getEntryName
          val jarFile = con.getJarFile
          jarFile.entries.filter(_.getName.startsWith(entryName)).foreach { e =>
            val dst = dir / e.getName.drop(entryName.size)
            created.getOrElseUpdate(dst, {
              if (e.isDirectory) {
                createDirectory(dst)
              } else {
                createFile(dst, IO.readStream(jarFile.getInputStream(e)))
              }
              true
            })
          }
      }
    )
  }
}

trait RevokableActions extends TemplateActions {
  def mode: Mode.Value

  private val _functions = collection.mutable.ListBuffer[() => Unit]()
  private var _record = false
  def recordInvocations(f: => Unit): Seq[() => Any] = {
    _functions.clear
    _record = true
    f
    _record = false
    _functions.toSeq
  }

  private def execute(f: => Status) = if (_record) {
    _functions.append(f _)
    Status.Record
  } else {
    f
  }

  override def createDirectory(dir: File): Status = execute {
    mode match {
      case Mode.Invoke => super.createDirectory(dir)
      case Mode.Revoke => removeDirectory(dir)
    }
  }

  override def copyDirectory(source: File, destination: File): Seq[File] = {
    if (_record) {
      _functions.append(() => copyDirectory(source, destination))
      Nil
    } else {
      mode match {
        case Mode.Invoke => super.copyDirectory(source, destination)
        case Mode.Revoke => revokeCopyDirectory(source, destination)
      }
    }
  }

  def revokeCopyDirectory(source: File, destination: File): Seq[File] = {
    val l = source.getPath.length
    source.***.get.tail.reverse.map { src =>
      val dst = destination / src.getPath.drop(l)
      if (src.isDirectory) {
        removeDirectory(dst)
      } else {
        removeFile(dst)
      }
      dst
    }
  }

  override def createFile(file: File, data: => String): Status = execute {
    mode match {
      case Mode.Invoke => super.createFile(file, data)
      case Mode.Revoke => removeFile(file)
    }
  }

  override def copyFile(source: File, destination: File): Status = execute {
    mode match {
      case Mode.Invoke => super.copyFile(source, destination)
      case Mode.Revoke => removeFile(destination)
    }
  }

  override def template(destination: File, resource: String, args: Map[String, Any]): Status = execute {
    mode match {
      case Mode.Invoke => super.template(destination, resource, args)
      case Mode.Revoke => removeFile(destination)
    }
  }

  override def insertIntoFileBefore(file: File, regex: String, data: => String): Status = execute {
    mode match {
      case Mode.Invoke => super.insert(file, regex, data, after = false)
      case Mode.Revoke => revokeInsert(file, regex, data, after = false)
    }
  }

  override def insertIntoFileAfter(file: File, regex: String, data: => String): Status = execute {
    mode match {
      case Mode.Invoke => super.insert(file, regex, data, after = true)
      case Mode.Revoke => revokeInsert(file, regex, data, after = true)
    }
  }
  
  protected def revokeInsert(file: File, regex: String, data: => String,
    after: Boolean): Status = {
    val d = data
    val content = IO.read(file)
    val (r, replacement) = if (after) {
      (s"(${regex})${Pattern.quote(d)}", "$1")
    } else {
      (s"${Pattern.quote(d)}(${regex})", "$1")
    }

    val replaced = content.replaceFirst(r, replacement)
    val status = if (replaced != content) {
      IO.write(file, replaced)
      Status.Subtract
    } else {
      Status.Skip
    }

    log(status, file)
  }
}