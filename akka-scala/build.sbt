import sbt.Keys.libraryDependencies
import scala.util.Try

ThisBuild / organization := "com.lunatech"
ThisBuild / version := Try(IO.readLines(baseDirectory.value / "version").mkString).toOption.getOrElse("unknown")
ThisBuild / scalaVersion := "2.13.4"

val `lunatech-energy-monitoring-demo` = project.in(file("."))
  .aggregate(`digital-twins`)

lazy val `digital-twins` = project.in(file("digital-twins"))
  .enablePlugins(AkkaGrpcPlugin, JavaAppPackaging)
  .settings(
    fork := true,
    libraryDependencies ++= Dependencies.digitalTwinsDependencies
  )
  .settings(
    Universal / mappings ++=
      Seq(
        file("lib/librpi_ws281x.so") -> "lib/librpi_ws281x.so",
        file("lib/librpi_ws281x_64.so") -> "lib/librpi_ws281x_64.so"
      ),
    Universal / javaOptions ++=
      Seq(
        "-Djava.library.path=lib",
        "-Dcluster-node-configuration.cluster-id=cluster-0",
        "-Dcluster-status-indicator.led-strip-type=ten-led-non-reversed-order"
      )
  )
  .settings(commonSettings)

lazy val simulator = project.in(file("simulator"))
  .enablePlugins(AkkaGrpcPlugin)
  .settings(
    fork := true,
    libraryDependencies ++= Dependencies.simluatorDependencies
  )

lazy val commonSettings = Seq(
  Compile  / scalacOptions ++= compileOptions,
  Compile / javacOptions ++= Seq("--release", "11")
)

val compileOptions = Seq(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-Xlint",
  "-encoding", "UTF-8"
)
