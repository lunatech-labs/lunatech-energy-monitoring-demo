package com.lunatech.energy.demo

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior, Terminated}
import akkapi.cluster._
import com.typesafe.config.Config

import java.time.Clock

object Main {

  def main(args: Array[String]): Unit = {
    val osName = System.getProperty("os.name")
    println(s"os.name = $osName")

    if (osName != "Mac OS X") {
      startPiCluster()
    } else {
      startLaptop()
    }
  }

  private def startLaptop(): Unit = {
    val settings = Settings("laptop.conf")
    start(settings.config, Behaviors.empty)
  }

  private def startPiCluster(): Unit = {
    val osArch = System.getProperty("os.arch")
    println(s"os.arch = $osArch")

    if (System.getProperty("os.arch") == "aarch64") {
      println(s"Running on a 64-bit architecture")
      System.loadLibrary("rpi_ws281x_64")
    } else {
      println(s"Running on a 32-bit architecture")
      System.loadLibrary("rpi_ws281x")
    }

    val settings = Settings("application.conf")
    start(settings.config, behaviour(settings))
  }

  private def start(config: Config, behavior: Behavior[Nothing]): Unit = {
    val system = ActorSystem[Nothing](behavior, "digital-twins", config)

    MachineShardingRegion.init(system, Clock.systemUTC())
    DigitalTwinProjections.init(system)

    new DigitalTwinServer(system).run()
  }

  private def behaviour(settings: Settings): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    val ledStripDriver = context.spawn(LedStripDriver(settings), "led-strip-driver")
    val ledStripController = context.spawn(LedStripVisualiser(settings, ledStripDriver), "led-strip-controller")
    val clusterStatusTracker =
      context.spawn(
        ClusterStatusTracker(
          settings,
          Some(contextToClusterSingleton(settings))
        ),
        "cluster-status-tracker"
      )
    clusterStatusTracker ! ClusterStatusTracker.SubscribeVisualiser(ledStripController)

    Behaviors.receiveSignal[Nothing] {
      case (_, Terminated(_)) =>
        Behaviors.stopped
    }
  }

  private def contextToClusterSingleton(settings: Settings): ActorContext[ClusterStatusTracker.ClusterEvent] => Behavior[PiClusterSingleton.Command]  =
    (context: ActorContext[ClusterStatusTracker.ClusterEvent]) => PiClusterSingleton(settings, context.self)
}
