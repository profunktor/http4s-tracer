resolvers += Classpaths.sbtPluginReleases
resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"
addSbtPlugin("com.geirsson"              % "sbt-ci-release" % "1.5.7")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat"   % "0.1.20")
addSbtPlugin("de.heikoseeberger"         % "sbt-header"     % "5.6.0")
addSbtPlugin("com.lucidchart"            % "sbt-scalafmt"   % "1.16")
addSbtPlugin("org.tpolecat"              % "tut-plugin"     % "0.6.13")
addSbtPlugin("com.47deg"                 % "sbt-microsites" % "1.2.1")
addSbtPlugin("com.scalapenos"            % "sbt-prompt"     % "1.0.2")
