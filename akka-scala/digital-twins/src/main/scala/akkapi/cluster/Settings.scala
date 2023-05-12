package akkapi.cluster

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import org.neopixel.Neopixel

import scala.concurrent.duration.{Duration, FiniteDuration, MILLISECONDS => Millis}
import scala.jdk.CollectionConverters._

object Settings {

  def apply(configResource: String): Settings = {
    val conf = ConfigFactory.load(configResource)

    println(s"""POSTGRES_HOST = ${System.getenv("POSTGRES_HOST")} = ${conf.getString("slick.db.host")}""")

    val config = if (conf.hasPath("cluster-node-configuration.node-hostname")) {
      val nodeHostname = conf.getString("cluster-node-configuration.node-hostname")
      conf
        .withValue("akka.remote.artery.canonical.hostname", ConfigValueFactory.fromAnyRef(nodeHostname))
        .withValue("akka.http.server.preview.enable-http2", ConfigValueFactory.fromAnyRef("on"))
        .withValue("slick.db.host", ConfigValueFactory.fromAnyRef("192.168.1.100"))
        .resolve()
    } else {
      conf.withValue("akka.http.server.preview.enable-http2", ConfigValueFactory.fromAnyRef("on"))
    }

    println(config.getString("akka.remote.artery.canonical.hostname"))
    println(config.getString("slick.db.host"))
    println(config.getString("slick.db.url"))

    new Settings(config)
  }
}

class Settings(val config: Config) {

  private val colorMap: Map[String, Long] = Neopixel.availableColorMap.map { case (x, y) => (x.toUpperCase, y)}

  private def validateColor(config: Config, colorSetting: String): Long = {
    val color = config.getString(colorSetting).toUpperCase
    if (colorMap.contains(color)) {
      colorMap(color)
    } else throw new Exception(s"$color: invalid color for $colorSetting")
  }

  private val clusterNodeConfig = config.getConfig("cluster-node-configuration")

  val actorSystemName = s"pi-${config.getString("cluster-node-configuration.cluster-id")}-system"

  private val clusterId = clusterNodeConfig.getString("cluster-id")

  private val clusterNodeToLedMapping = clusterNodeConfig.getConfig(s"cluster-node-to-led-mapping.$clusterId")

  val HostToLedMapping: Map[String, Int] = (for {
    mapping <- clusterNodeToLedMapping.entrySet().asScala
  } yield (mapping.getKey, clusterNodeToLedMapping.getInt(mapping.getKey))).toMap

  val nodeUpColor: Long =
    validateColor(config, "cluster-status-indicator.cluster-node-colors.cluster-node-up-color")

  val nodeWeaklyUpColor: Long =
    validateColor(config, "cluster-status-indicator.cluster-node-colors.cluster-node-weakly-up-color")

  val nodeDownColor: Long =
    validateColor(config, "cluster-status-indicator.cluster-node-colors.cluster-node-down-color")

  val nodeLeftColor: Long =
    validateColor(config, "cluster-status-indicator.cluster-node-colors.cluster-node-left-color")

  val nodeExitedColor: Long =
    validateColor(config, "cluster-status-indicator.cluster-node-colors.cluster-node-exited-color")

  val nodeUnreachableColor: Long =
    validateColor(config, "cluster-status-indicator.cluster-node-colors.cluster-node-unreachable-color")

  val nodeJoinedColor: Long =
    validateColor(config, "cluster-status-indicator.cluster-node-colors.cluster-node-joined-color")

  val singletonIndicatorColor: Long =
    validateColor(config, "cluster-status-indicator.cluster-node-colors.cluster-node-singleton-indicator-color")

  val leaderIndicatorColor: Long =
    validateColor(config, "cluster-status-indicator.cluster-leader-indicator-color")

  val heartbeatIndicatorColor: Long =
    validateColor(config, "cluster-status-indicator.cluster-heartbeat-indicator-color")

  val heartbeatIndicatorConvergenceColor: Long =
    validateColor(config, "cluster-status-indicator.cluster-heartbeat-indicator-convergence-color")

  val heartbeatIndicatorNoConvergenceColor: Long =
    validateColor(config, "cluster-status-indicator.cluster-heartbeat-indicator-no-convergence-color")

  val heartbeatIndicatorInterval: FiniteDuration =
    Duration(config.getDuration("cluster-status-indicator.cluster-heartbeat-indicator-interval", Millis), Millis)

  val weaklyUpIndicatorInterval: FiniteDuration =
    Duration(config.getDuration("cluster-status-indicator.cluster-weakly-up-indicator-interval", Millis), Millis)

  val clusterStateConvergenceInterval: FiniteDuration =
    Duration(config.getDuration("cluster-status-indicator.cluster-heartbeat-indicator-convergence-interval", Millis), Millis)

  // Mapping of different status indicators to their logical position on the LED strip
  val LeaderLedNumber = 5
  val SingletonLedNumber = 6
  val HeartbeatLedNumber = 7

  object LedStripConfig {

    val ledBrightness: Short =
      config.getInt("cluster-status-indicator.led-brightness").toShort

    val ledCount: Int =
      config.getInt("cluster-status-indicator.led-count")

    val ledPin: Int =
      config.getInt("cluster-status-indicator.led-pin")

    val ledFreqHz: Int =
      config.getInt("cluster-status-indicator.led-freq-hz")

    val ledDma: Int =
      config.getInt("cluster-status-indicator.led-dma")

    val ledInvert: Boolean =
      config.getBoolean("cluster-status-indicator.led-invert")

    val ledChannel: Int =
      config.getInt("cluster-status-indicator.led-channel")
  }
}

