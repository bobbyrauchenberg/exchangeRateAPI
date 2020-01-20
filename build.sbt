val Http4sVersion = "0.20.15"
val CirceVersion = "0.11.1"
val Specs2Version = "4.1.0"
val LogbackVersion = "1.2.3"
val CatsScalaTestVersion = "3.0.0"
val ScalacacheVersion = "0.28.0"
val ScalatestVersion = "3.1.0"
val CatsScalatestVersion = "3.0.4"
val ScalacheckVersion = "3.1.0.0"
val RandomDataGeneratorVersion = "2.7"
val WiremockVersion = "2.25.1"


lazy val root = (project in file("."))
  .settings(
    organization := "com.rauchenberg",
    name := "exchangeRateAPI",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.10",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "io.circe" %% "circe-core" % CirceVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "com.github.cb372" %% "scalacache-cats-effect" % ScalacacheVersion,
      "com.github.cb372" %% "scalacache-core" % ScalacacheVersion,
      "com.github.cb372" %% "scalacache-caffeine" % ScalacacheVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "org.scalatest" %% "scalatest" % ScalatestVersion % Test,
      "org.scalatestplus" %% "scalacheck-1-14" % ScalacheckVersion % Test,
      "com.danielasfregola" %% "random-data-generator-magnolia" % RandomDataGeneratorVersion % Test,
      "com.ironcorelabs" %% "cats-scalatest" % CatsScalaTestVersion % Test,
      "com.github.tomakehurst" % "wiremock" % WiremockVersion % Test,
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0")
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Ypartial-unification",
  "-Xfatal-warnings",
)
