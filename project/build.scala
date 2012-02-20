import sbt._
import Keys._

object SwaggerExtBuild extends Build {
  lazy override val projects = Seq(root)

  lazy val root = Project("swagger-ext", file("."), settings = Defaults.defaultSettings ++ Seq(
    scalaSource in Compile <<= baseDirectory { (base) => base / "src" },
    libraryDependencies ++= Seq(
      "wordnik" % "swagger-libs-gen" % "1.1-SHAPSHOT.121026" % "compile->build",
      "net.liftweb" %% "lift-json" % "2.4"
    )
  ))
}