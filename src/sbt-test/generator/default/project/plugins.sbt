val _version = Option(System.getProperty("version")).getOrElse(
  throw new RuntimeException("The system property 'version' is not defined.")
)

addSbtPlugin("com.github.aselab" % "sbt-generator" % _version)
