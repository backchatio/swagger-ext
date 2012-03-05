import sbt._
import Keys._

object SwaggerExtBuild extends Build {
  lazy override val projects = Seq(root)

  lazy val root = Project("swagger-ext", file("."), settings = Defaults.defaultSettings ++ Seq(
    scalaSource in Compile <<= baseDirectory { (base) => base / "src" },
    libraryDependencies ++= Seq(
      "org.scala-tools.time" %% "time" % "0.5",
      "wordnik" % "swagger-libs-gen" % "1.1-SHAPSHOT.121026" % "compile->build",
      "net.liftweb" %% "lift-json" % "2.4",
      "net.liftweb" %% "lift-json-ext" % "2.4",
      "com.ning" % "async-http-client" % "1.7.0",
      "com.github.scala-incubator.io" %% "scala-io-core" % "0.3.0",
      "com.mojolly.inflector" %% "scala-inflector" % "1.3.1-SNAPSHOT"
    ),
    resolvers += "Sonatype" at "https://oss.sonatype.org/content/repositories/snapshots/"
  ))
}