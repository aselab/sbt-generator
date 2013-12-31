package com.github.aselab.sbt

import org.specs2.mutable._

class GeneratorSpec extends Specification {
  "GeneratorCompanion" should {
    def companion = new GeneratorCompanion {}

    "apply not registered generator" in {
      companion.apply("registered") must throwA[RuntimeException]
    }

    "apply registered generator" in {
      val generator = new NoArgGenerator("registered") { def generate =  {} }
      val c = companion
      c.register(generator)
      c.apply("registered") mustEqual generator
    }

    "register a generator twice" in {
      val generator = new NoArgGenerator("g") { def generate =  {} }
      val c = companion
      c.register(generator)
      c.register(generator) mustEqual generator
    }

    "register same name generators" in {
      Generator.register(new NoArgGenerator("g") { def generate =  {} })
      Generator.register(new NoArgGenerator("g") { def generate =  {} }) must throwA[RuntimeException]
    }
  }
}
