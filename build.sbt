import com.scalapenos.sbt.prompt.SbtPrompt.autoImport._
import com.scalapenos.sbt.prompt._
import Dependencies._
import microsites.ExtraMdFileConfig

name := """https-tracer-root"""

organization in ThisBuild := "com.github.gvolpe"

crossScalaVersions in ThisBuild := Seq("2.11.12", "2.12.8")

sonatypeProfileName := "com.github.gvolpe"

promptTheme := PromptTheme(List(
  text(_ => "[http4s-tracer]", fg(64)).padRight(" λ ")
 ))

lazy val commonSettings = Seq(
  startYear := Some(2018),
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/gvolpe/http4s-tracer")),
  headerLicense := Some(HeaderLicense.ALv2("2018-2019", "Gabriel Volpe")),
  libraryDependencies ++= Seq(
    compilerPlugin(Libraries.kindProjector),
    compilerPlugin(Libraries.betterMonadicFor),
    Libraries.catsEffect,
    Libraries.fs2Core,
    Libraries.http4sServer,
    Libraries.http4sDsl,
    Libraries.gfcTimeuuid,
    Libraries.scalaTest  % Test,
    Libraries.scalaCheck % Test
  ),
  resolvers += "Apache public" at "https://repository.apache.org/content/groups/public/",
  scalafmtOnCompile := true,
  publishTo := {
    val sonatype = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at sonatype + "content/repositories/snapshots")
    else
      Some("releases" at sonatype + "service/local/staging/deploy/maven2")
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra :=
      <developers>
        <developer>
          <id>gvolpe</id>
          <name>Gabriel Volpe</name>
          <url>http://github.com/gvolpe</url>
        </developer>
      </developers>
)

lazy val examplesDependencies = Seq(
  Libraries.catsPar,
  Libraries.http4sClient,
  Libraries.http4sCirce,
  Libraries.circeCore,
  Libraries.circeGeneric,
  Libraries.circeGenericX,
  Libraries.zioCore,
  Libraries.zioCats,
  Libraries.log4CatsSlf4j,
  Libraries.logback % Runtime,
)

lazy val root = project.in(file("."))
  .aggregate(`http4s-tracer`, `http4s-tracer-log4cats`, examples, microsite)
  .settings(noPublish)

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  skip in publish := true
)

lazy val `http4s-tracer` = project.in(file("core"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies += Libraries.http4sClient % Test)
  .enablePlugins(AutomateHeaderPlugin)

lazy val `http4s-tracer-log4cats` = project.in(file("log4cats"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies += Libraries.log4CatsCore)
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(`http4s-tracer`)

lazy val examples = project.in(file("examples"))
  .settings(commonSettings: _*)
  .settings(scalacOptions -= "-Ywarn-dead-code")
  .settings(libraryDependencies ++= examplesDependencies)
  .settings(noPublish)
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(`http4s-tracer`, `http4s-tracer-log4cats`)

lazy val microsite = project.in(file("site"))
  .enablePlugins(MicrositesPlugin)
  .settings(commonSettings: _*)
  .settings(noPublish)
  .settings(
    micrositeName := "Http4s Tracer",
    micrositeDescription := "End-to-end tracing system for Http4s",
    micrositeAuthor := "Gabriel Volpe",
    micrositeGithubOwner := "gvolpe",
    micrositeGithubRepo := "http4s-tracer",
    micrositeBaseUrl := "/http4s-tracer",
    micrositeExtraMdFiles := Map(
      file("README.md") -> ExtraMdFileConfig(
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
      "brand-primary"     -> "#E05236",
      "brand-secondary"   -> "#631224",
      "brand-tertiary"    -> "#2D232F",
      "gray-dark"         -> "#453E46",
      "gray"              -> "#837F84",
      "gray-light"        -> "#E3E2E3",
      "gray-lighter"      -> "#F4F3F4",
      "white-color"       -> "#FFFFFF"
    ),
    micrositeGitterChannel := true,
    micrositeGitterChannelUrl := "http4s-tracer/http4s-tracer",
    micrositePushSiteWith := GitHub4s,
    micrositeGithubToken := sys.env.get("GITHUB_TOKEN"),
    fork in tut := true,
    scalacOptions in Tut --= Seq(
      "-Xfatal-warnings",
      "-Ywarn-unused-import",
      "-Ywarn-numeric-widen",
      "-Ywarn-dead-code",
      "-Xlint:-missing-interpolator,_",
    )
  )
  .dependsOn(`http4s-tracer`, examples)

// CI build
addCommandAlias("buildHttp4sTracer", ";clean;+test;tut")
