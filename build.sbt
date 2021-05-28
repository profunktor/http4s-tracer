import com.scalapenos.sbt.prompt._
import com.scalapenos.sbt.prompt.SbtPrompt.autoImport._
import Dependencies._
import microsites.ExtraMdFileConfig

name := """https-tracer"""

ThisBuild / organization := "dev.profunktor"
ThisBuild / crossScalaVersions := List("2.12.14", "2.13.2")

promptTheme := PromptTheme(
  List(
    text(_ => "[http4s-tracer]", fg(64)).padRight(" λ ")
  )
)

def uuidDep(v: String): List[ModuleID] =
  CrossVersion.partialVersion(v) match {
    case Some((2, 13)) => List.empty
    case _             => List(Libraries.gfcTimeuuid)
  }

inThisBuild(
  List(
    organization := "dev.profunktor",
    homepage := Some(url("https://profunktor.dev/")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "gvolpe",
        "Gabriel Volpe",
        "volpegabriel@gmail.com",
        url("https://gvolpe.github.io")
      )
    )
  )
)

lazy val commonSettings = List(
  startYear := Some(2018),
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://http4s-tracer.profunktor.dev/")),
  headerLicense := Some(HeaderLicense.ALv2("2018-2020", "ProfunKtor")),
  testFrameworks += new TestFramework("munit.Framework"),
  libraryDependencies ++= List(
    compilerPlugin(Libraries.kindProjector),
    compilerPlugin(Libraries.betterMonadicFor),
    Libraries.catsEffect,
    Libraries.fs2Core,
    Libraries.http4sServer,
    Libraries.http4sDsl,
    Libraries.munitCore       % Test,
    Libraries.munitScalacheck % Test
  ),
  libraryDependencies ++= uuidDep(scalaVersion.value),
  resolvers += "Apache public" at "https://repository.apache.org/content/groups/public/",
  scalafmtOnCompile := true
)

lazy val examplesDependencies = List(
  Libraries.http4sClient,
  Libraries.http4sCirce,
  Libraries.circeCore,
  Libraries.circeGeneric,
  Libraries.circeGenericX,
  Libraries.zioCore,
  Libraries.zioCats,
  Libraries.log4CatsSlf4j,
  Libraries.logback % Runtime
)

lazy val root = project
  .in(file("."))
  .aggregate(`http4s-tracer`, `http4s-tracer-log4cats`, examples, microsite)
  .settings(noPublish)

lazy val noPublish = List(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  skip in publish := true
)

lazy val `http4s-tracer` = project
  .in(file("core"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies += Libraries.http4sClient % Test)
  .enablePlugins(AutomateHeaderPlugin)

lazy val `http4s-tracer-log4cats` = project
  .in(file("log4cats"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies += Libraries.log4CatsCore)
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(`http4s-tracer`)

lazy val examples = project
  .in(file("examples"))
  .settings(commonSettings: _*)
  .settings(scalacOptions -= "-Ywarn-dead-code")
  .settings(libraryDependencies ++= examplesDependencies)
  .settings(noPublish)
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(`http4s-tracer`, `http4s-tracer-log4cats`)

lazy val microsite = project
  .in(file("site"))
  .enablePlugins(MicrositesPlugin)
  .settings(commonSettings: _*)
  .settings(noPublish)
  .settings(
    micrositeName := "Http4s Tracer",
    micrositeDescription := "End-to-end tracing system for Http4s",
    micrositeAuthor := "ProfunKtor",
    micrositeGithubOwner := "profunktor",
    micrositeGithubRepo := "http4s-tracer",
    micrositeBaseUrl := "",
    micrositeExtraMdFiles := Map(
      file("README.md")          -> ExtraMdFileConfig(
        "index.md",
        "home"
      ),
      file("CODE_OF_CONDUCT.md") -> ExtraMdFileConfig(
        "CODE_OF_CONDUCT.md",
        "page",
        Map("title" -> "Code of Conduct")
      )
    ),
    micrositePalette := Map(
      "brand-primary"   -> "#E05236",
      "brand-secondary" -> "#631224",
      "brand-tertiary"  -> "#2D232F",
      "gray-dark"       -> "#453E46",
      "gray"            -> "#837F84",
      "gray-light"      -> "#E3E2E3",
      "gray-lighter"    -> "#F4F3F4",
      "white-color"     -> "#FFFFFF"
    ),
    micrositeGitterChannel := true,
    micrositeGitterChannelUrl := "profunktor-dev/http4s-tracer",
    fork in tut := true,
    scalacOptions in Tut --= List(
      "-Xfatal-warnings",
      "-Ywarn-unused-import",
      "-Ywarn-numeric-widen",
      "-Ywarn-dead-code",
      "-Xlint:-missing-interpolator,_"
    )
  )
  .dependsOn(`http4s-tracer`, examples)

// CI build
addCommandAlias("buildHttp4sTracer", ";clean;+test;tut")
