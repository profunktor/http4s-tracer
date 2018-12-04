import sbt._

object Dependencies {

  object Versions {
    val catsEffect  = "1.1.0"
    val fs2         = "1.0.1"
    val http4s      = "0.20.0-M3"
    val circe       = "0.10.1"
    val gfcTimeuuid = "0.0.8"

    // Test
    val scalaTest   = "3.0.5"
    val scalaCheck  = "1.14.0"

    // Compiler
    val kindProjector     = "0.9.9"
    val betterMonadicFor  = "0.2.4"

    // Runtime
    val logback     = "1.2.1"
  }

  object Libraries {
    def circe(artifact: String): ModuleID   = "io.circe"    %% artifact % Versions.circe
    def http4s(artifact: String): ModuleID  = "org.http4s"  %% artifact % Versions.http4s

    lazy val catsEffect     = "org.typelevel"         %% "cats-effect"      % Versions.catsEffect
    lazy val fs2Core        = "co.fs2"                %% "fs2-core"         % Versions.fs2

    lazy val http4sServer   = http4s("http4s-blaze-server")
    lazy val http4sClient   = http4s("http4s-blaze-client")
    lazy val http4sDsl      = http4s("http4s-dsl")
    lazy val http4sCirce    = http4s("http4s-circe")

    lazy val circeCore      = circe("circe-core")
    lazy val circeGeneric   = circe("circe-generic")
    lazy val circeGenericX  = circe("circe-generic-extras")

    lazy val gfcTimeuuid    = "com.gilt"              %% "gfc-timeuuid"     % Versions.gfcTimeuuid

    // Test
    lazy val scalaCheck     = "org.scalacheck"        %% "scalacheck"       % Versions.scalaCheck
    lazy val scalaTest      = "org.scalatest"         %% "scalatest"        % Versions.scalaTest

    // Compiler
    lazy val kindProjector    = "org.spire-math"      %% "kind-projector"     % Versions.kindProjector // cross CrossVersion.full
    lazy val betterMonadicFor = "com.olegpy"          %% "better-monadic-for" % Versions.betterMonadicFor

    // Runtime
    lazy val logback        = "ch.qos.logback"        %  "logback-classic"  % Versions.logback
  }

}
