name := "ScalaCatsApp"

version := "0.1.0"

scalaVersion := "2.13.15"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.5.1",
  "org.http4s" %% "http4s-blaze-server" % "0.23.12",
  "org.http4s" %% "http4s-blaze-client" % "0.23.12",
  "org.http4s" %% "http4s-circe" % "0.23.12",
  "org.http4s" %% "http4s-dsl" % "0.23.12",
  "io.circe" %% "circe-generic" % "0.14.5",
  "io.circe" %% "circe-literal" % "0.14.5",
  "org.tpolecat" %% "doobie-core" % "1.0.0-RC4",
  "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC4",
  "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC4",
  "org.postgresql" % "postgresql" % "42.6.0",
  "com.typesafe" % "config" % "1.4.2",
  "org.typelevel" %% "log4cats-slf4j" % "2.5.0",
  "ch.qos.logback" % "logback-classic" % "1.2.11"
)

ThisBuild / organization := "com.example"

ThisBuild / scalaVersion := "2.13.15"
