import com.scalapenos.sbt.prompt.SbtPrompt.autoImport._
import com.scalapenos.sbt.prompt._
import Dependencies._

name := """https-tracer-root"""

organization in ThisBuild := "com.github.gvolpe"

version in ThisBuild := "0.5"

crossScalaVersions in ThisBuild := Seq("2.11.12", "2.12.4")

sonatypeProfileName := "com.github.gvolpe"

promptTheme := PromptTheme(List(
  text(_ => "[http4s-tracer]", fg(64)).padRight(" λ ")
 ))

lazy val commonScalacOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:experimental.macros",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-value-discard",
  "-Xfuture",
  "-Xlog-reflective-calls",
  "-Ywarn-inaccessible",
  "-Ypatmat-exhaust-depth", "20",
  "-Ydelambdafy:method",
  "-Xmax-classfile-name", "100"
)

lazy val commonSettings = Seq(
  startYear := Some(2018),
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/gvolpe/https-tracer")),
  libraryDependencies ++= Seq(
    compilerPlugin(Libraries.kindProjector),
    Libraries.catsEffect,
    Libraries.fs2Core,
    Libraries.http4sServer,
    Libraries.http4sDsl,
    Libraries.gfcTimeuuid,
    Libraries.scalaTest  % Test,
    Libraries.scalaCheck % Test
  ),
  resolvers += "Apache public" at "https://repository.apache.org/content/groups/public/",
  scalacOptions := commonScalacOptions,
  scalafmtOnCompile := true
  /*coverageExcludedPackages := "com\\.github\\.gvolpe\\.fs2rabbit\\.examples.*;com\\.github\\.gvolpe\\.fs2rabbit\\.typeclasses.*;com\\.github\\.gvolpe\\.fs2rabbit\\.instances.*;.*QueueName*;.*RoutingKey*;.*ExchangeName*;.*DeliveryTag*;.*AmqpClientStream*;.*ConnectionStream*;",
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
      </developers>*/
)

lazy val examplesDependencies = Seq(
  Libraries.http4sClient,
  Libraries.http4sCirce,
  Libraries.circeCore,
  Libraries.circeGeneric,
  Libraries.circeGenericX,
  Libraries.logback % Runtime,
)

lazy val root = project.in(file("."))
  .aggregate(`http4s-tracer`, examples)
  .settings(noPublish)

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  skip in publish := true
)

lazy val `http4s-tracer` = project.in(file("core"))
  .settings(commonSettings: _*)
  .settings(parallelExecution in Test := false)
  .enablePlugins(AutomateHeaderPlugin)

lazy val examples = project.in(file("examples"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= examplesDependencies)
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(`http4s-tracer`)
