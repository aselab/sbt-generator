package com.github.aselab.sbt

import sbt._
import org.fusesource.scalate._

class ScalateTemplate(libraryJar: File, templateDir: File, loader: ClassLoader){
  lazy val engine = {
    val mode = System.getProperty("scalate.mode", "production")
    val engine = new TemplateEngine(Nil, mode)
    engine.combinedClassPath = true
    engine.classpath = libraryJar.getAbsolutePath
    engine
  }

  def render(templatePath: String, attributes: Map[String, Any]): String = {
    val file = templateDir / templatePath
    val source = if (file.exists) {
      TemplateSource.fromFile(file)
    } else {
      TemplateSource.fromURL(loader.getResource("templates/" + templatePath))
    }
    engine.layout(source, attributes)
  }
}

