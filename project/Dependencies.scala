import sbt._

object Dependencies {

  object V {
    val catsEffect  = "2.5.0"
    val fs2         = "2.5.5"
    val http4s      = "0.21.24"
    val circe       = "0.13.0"
    val gfcTimeuuid = "0.0.8"
    val log4Cats    = "1.1.1"
    val zio         = "1.0.7"
    val zioCats     = "2.4.1.0"

    // Test
    val munit = "0.7.25"

    // Compiler
    val kindProjector    = "0.11.0"
    val betterMonadicFor = "0.3.1"

    // Runtime
    val logback = "1.2.3"
  }

  object Libraries {
    def circe(artifact: String): ModuleID                = "io.circe"          %% s"circe-$artifact"    % V.circe
    def http4s(artifact: String): ModuleID               = "org.http4s"        %% s"http4s-$artifact"   % V.http4s
    def log4cats(artifact: String): ModuleID             = "io.chrisdavenport" %% s"log4cats-$artifact" % V.log4Cats
    def zio(artifact: String, version: String): ModuleID = "dev.zio"           %% artifact              % version

    val catsEffect = "org.typelevel" %% "cats-effect" % V.catsEffect
    val fs2Core    = "co.fs2"        %% "fs2-core"    % V.fs2

    val http4sServer = http4s("blaze-server")
    val http4sClient = http4s("blaze-client")
    val http4sDsl    = http4s("dsl")
    val http4sCirce  = http4s("circe")

    val circeCore     = circe("core")
    val circeGeneric  = circe("generic")
    val circeGenericX = circe("generic-extras")

    val zioCore = zio("zio", V.zio)
    val zioCats = zio("zio-interop-cats", V.zioCats)

    val gfcTimeuuid = "com.gilt" %% "gfc-timeuuid" % V.gfcTimeuuid

    val log4CatsCore  = log4cats("core")
    val log4CatsSlf4j = log4cats("slf4j")

    // Test
    val munitCore       = "org.scalameta" %% "munit"            % V.munit
    val munitScalacheck = "org.scalameta" %% "munit-scalacheck" % V.munit

    // Compiler
    val kindProjector    = "org.typelevel" %% "kind-projector"     % V.kindProjector cross CrossVersion.full
    val betterMonadicFor = "com.olegpy"    %% "better-monadic-for" % V.betterMonadicFor

    // Runtime
    val logback = "ch.qos.logback" % "logback-classic" % V.logback
  }

}
