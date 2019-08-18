import sbt._

object Dependencies {

  object Versions {
    val catsPar     = "0.2.1"
    val catsEffect  = "1.4.0"
    val fs2         = "1.0.5"
    val http4s      = "0.20.10"
    val circe       = "0.11.1"
    val gfcTimeuuid = "0.0.8"
    val log4Cats    = "0.3.0"
    val zio         = "1.0-RC5"

    // Test
    val scalaTest  = "3.0.8"
    val scalaCheck = "1.14.0"

    // Compiler
    val kindProjector    = "0.10.3"
    val betterMonadicFor = "0.3.1"

    // Runtime
    val logback = "1.2.3"
  }

  object Libraries {
    def circe(artifact: String): ModuleID    = "io.circe"          %% s"circe-$artifact"    % Versions.circe
    def http4s(artifact: String): ModuleID   = "org.http4s"        %% s"http4s-$artifact"   % Versions.http4s
    def log4cats(artifact: String): ModuleID = "io.chrisdavenport" %% s"log4cats-$artifact" % Versions.log4Cats
    def zio(artifact: String): ModuleID      = "org.scalaz"        %% artifact              % Versions.zio

    lazy val catsPar    = "io.chrisdavenport" %% "cats-par"    % Versions.catsPar
    lazy val catsEffect = "org.typelevel"     %% "cats-effect" % Versions.catsEffect
    lazy val fs2Core    = "co.fs2"            %% "fs2-core"    % Versions.fs2

    lazy val http4sServer = http4s("blaze-server")
    lazy val http4sClient = http4s("blaze-client")
    lazy val http4sDsl    = http4s("dsl")
    lazy val http4sCirce  = http4s("circe")

    lazy val circeCore     = circe("core")
    lazy val circeGeneric  = circe("generic")
    lazy val circeGenericX = circe("generic-extras")

    lazy val zioCore = zio("scalaz-zio")
    lazy val zioCats = zio("scalaz-zio-interop-cats")

    lazy val gfcTimeuuid = "com.gilt" %% "gfc-timeuuid" % Versions.gfcTimeuuid

    lazy val log4CatsCore  = log4cats("core")
    lazy val log4CatsSlf4j = log4cats("slf4j")

    // Test
    lazy val scalaCheck = "org.scalacheck" %% "scalacheck" % Versions.scalaCheck
    lazy val scalaTest  = "org.scalatest"  %% "scalatest"  % Versions.scalaTest

    // Compiler
    lazy val kindProjector    = "org.typelevel" %% "kind-projector"     % Versions.kindProjector // cross CrossVersion.full
    lazy val betterMonadicFor = "com.olegpy"     %% "better-monadic-for" % Versions.betterMonadicFor

    // Runtime
    lazy val logback = "ch.qos.logback" % "logback-classic" % Versions.logback
  }

}
