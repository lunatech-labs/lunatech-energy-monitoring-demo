package com.lunatech.energy.demo

import akka.Done
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardedDaemonProcessSettings}
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.eventsourced.EventEnvelope
import akka.projection.{ProjectionBehavior, ProjectionContext, ProjectionId}
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.SourceProvider
import akka.projection.slick.SlickProjection
import akka.stream.scaladsl.{Flow, FlowWithContext, Keep, Source}
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.scala.InfluxDBClientScalaFactory
import com.influxdb.client.write.Point
import com.lunatech.energy.demo.Machine.{MachineCreated, MachineEvent, MachineStatus, MachineStatusChanged}
import org.slf4j.LoggerFactory
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import java.time.{Instant, OffsetDateTime}
import scala.concurrent.Future

object DigitalTwinProjections {

  trait ProjectionEventHandler[E <: JsonSerializable] {
    def process(envelope: EventEnvelope[E]): Future[Done]
  }

  final class LoggingEventHandler extends ProjectionEventHandler[MachineEvent] {

    private val log = LoggerFactory.getLogger(getClass)

    override def process(envelope: EventEnvelope[MachineEvent]): Future[Done] = {
      log.info(s"handling event [${envelope.persistenceId}] => ${envelope.event}")
      Future.successful(Done)
    }
  }

  final class InfluxDbEventHandler(system: ActorSystem[_]) extends ProjectionEventHandler[MachineEvent] {
    import InfluxDbEventHandler._

    private val log = LoggerFactory.getLogger(getClass)

    override def process(envelope: EventEnvelope[MachineEvent]): Future[Done] = {
      log.info(s"handling event [${envelope.persistenceId}] => ${envelope.event}")

      envelope.event match {
        case MachineCreated(_, _) => Future.successful(Done)
        case MachineStatusChanged(currentStatus, changedAt) => handleMachineStatusChange(currentStatus, changedAt, envelope.persistenceId)(system)
      }
    }

    private def handleMachineStatusChange(status: Machine.MachineStatus, time: OffsetDateTime, persistenceId: String)(implicit system: ActorSystem[_]): Future[Done] = {

      val watts = status match {
        case MachineStatus.Working => 60000
        case MachineStatus.Standby => 700
        case MachineStatus.Off => 20
      }

      val point = Point
        .measurement("energy")
        .addTag("machine_id", persistenceId)
        .addField("used_kwh", watts)
        .time(time.toInstant, WritePrecision.NS)

      val flow = Flow[Point].map { elem =>
        log.info(s"recording point => $elem")
        elem
      }

      val sourcePoint = Source.single(point)
      val sinkPoint = client.getWriteScalaApi.writePoint()
      sourcePoint.via(flow).toMat(sinkPoint)(Keep.right).run()
    }
  }

  object InfluxDbEventHandler {
    private val client = InfluxDBClientScalaFactory.create(
      "http://192.168.1.100:8086", "LZl6JXnsS4ko_FxFx2nxrQ9xmSMS23zrDxBt4s8u_IClWJtBfJze1ZHFfCT45RNW-9SBMCttsD2XLs1K4mQQ2w==".toCharArray, "influxdata-org", "default")
  }

  val tags = Seq(
    "tag-1","tag-2","tag-3","tag-4","tag-5","tag-6","tag-7","tag-8","tag-9","tag-10",
  )

  def init(system: ActorSystem[_]): Unit = {
    val name = "digital-twin-projection"
    val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("slick", system.settings.config)
    val handler = new InfluxDbEventHandler(system)

    val shardingSettings = ClusterShardingSettings(system)
    val shardedDaemonProcessSettings = ShardedDaemonProcessSettings(system).withShardingSettings(shardingSettings)

    ShardedDaemonProcess(system).init[ProjectionBehavior.Command](
      name = name,
      numberOfInstances = tags.size,
      behaviorFactory = (index: Int) => createProjectionBehavior(dbConfig, name, handler, tag = tags(index))(system),
      shardedDaemonProcessSettings,
      stopMessage = Some(ProjectionBehavior.Stop))
  }

  private def createProjectionBehavior[E <: JsonSerializable](
                                                               dbConfig: DatabaseConfig[JdbcProfile],
                                                               name: String,
                                                               handler: ProjectionEventHandler[E],
                                                               tag: String)(implicit system: ActorSystem[_]): Behavior[ProjectionBehavior.Command] = {

    val sourceProvider: SourceProvider[Offset, EventEnvelope[E]] =
      EventSourcedProvider.eventsByTag[E](system, readJournalPluginId = JdbcReadJournal.Identifier, tag = tag)

    ProjectionBehavior {
      SlickProjection.atLeastOnceFlow(
        projectionId = ProjectionId(name, tag),
        sourceProvider = sourceProvider,
        databaseConfig = dbConfig,
        handler = flow(handler))
    }
  }

  private def flow[E <: JsonSerializable](handler: ProjectionEventHandler[E])(implicit system: ActorSystem[_]) =
    FlowWithContext[EventEnvelope[E], ProjectionContext].mapAsync(parallelism = 1) { envelope =>
      handler.process(envelope).recoverWith {
        case t: Throwable =>
          system.log.error(s"Processing event ${envelope.event} failed", t)
          Future.failed(t)
      }(system.executionContext)
    }
}
