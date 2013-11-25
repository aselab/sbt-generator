import com.github.aselab.sbt.Keys._

generatorSettings

generators ++= Seq(sample.Sample1Generator, sample.Sample2Generator)
