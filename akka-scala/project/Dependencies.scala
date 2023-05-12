import sbt._

object Dependencies {

  private object Version {
    val akka = "2.8.1"
    val akkaHttp = "10.5.2"
    val akkaPersistenceInMemoryPlugin = "2.5.15.2"
    val akkaPersistenceJdbc = "5.2.1"
    val akkaProjections = "1.3.1"
    val enumeratum = "1.7.0"
    val influxdb = "6.7.0"
    val logbackVersion = "1.2.3"
    val pi4j = "1.2"
    val postgres = "42.3.4"
  }

  private val akka = Seq(
    "com.typesafe.akka" %% "akka-actor-typed",
    "com.typesafe.akka" %% "akka-stream",
    "com.typesafe.akka" %% "akka-stream-typed",
    "com.typesafe.akka" %% "akka-cluster-typed",
    "com.typesafe.akka" %% "akka-cluster-sharding-typed",
    "com.typesafe.akka" %% "akka-discovery",
    "com.typesafe.akka" %% "akka-serialization-jackson",
    "com.typesafe.akka" %% "akka-persistence-typed",
    "com.typesafe.akka" %% "akka-discovery",
    "com.typesafe.akka" %% "akka-pki"
  ).map(_ % Version.akka)

  private val akkaHttp = Seq(
    "com.typesafe.akka" %% "akka-http",
    "com.typesafe.akka" %% "akka-http2-support"
  ).map(_ % Version.akkaHttp)

  private val akkaPersistenceInMemoryPlugin = Seq(
    "com.github.dnvriend" %% "akka-persistence-inmemory"
  ).map(_ % Version.akkaPersistenceInMemoryPlugin)

  private val akkaPersistenceJdbc = Seq(
    "com.lightbend.akka" %% "akka-persistence-jdbc"
  ).map(_ % Version.akkaPersistenceJdbc)

  private val akkaProjections = Seq(
    "com.lightbend.akka" %% "akka-projection-eventsourced",
    "com.lightbend.akka" %% "akka-projection-slick"
  ).map(_ % Version.akkaProjections)

  private val akkaTest = Seq(
    "com.typesafe.akka" %% "akka-persistence-testkit"
  ).map(_ % Version.akka % Test)

  private val enumeratum = Seq(
    "com.beachape" %% "enumeratum"
  ).map(_ % Version.enumeratum)

  private val influxdb = Seq(
    "com.influxdb" %% "influxdb-client-scala"
  ).map(_ % Version.influxdb)

  private val logging = Seq(
    "ch.qos.logback" % "logback-classic"
  ).map(_ % Version.logbackVersion)

  private val pi4j = Seq(
    "com.pi4j" % "pi4j-core",
    "com.pi4j" % "pi4j-device",
    "com.pi4j" % "pi4j-gpio-extension"
  ).map (_ % Version.pi4j)

  private val postgres = Seq(
    "org.postgresql" % "postgresql"
  ).map(_ % Version.postgres)

  val digitalTwinsDependencies =
    akka ++
    akkaHttp ++
    akkaPersistenceInMemoryPlugin ++
    akkaPersistenceJdbc ++
    akkaProjections ++
    akkaTest ++
    enumeratum ++
    influxdb ++
    logging ++
    pi4j ++
    postgres

  val simluatorDependencies =
    akka ++
    akkaHttp ++
    logging
}
