package com.lunatech.energy.demo

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import enumeratum.{Enum, EnumEntry}
import org.slf4j.LoggerFactory

import java.time.{Clock, OffsetDateTime, ZoneOffset}
import scala.collection.immutable

object Machine {

  implicit class BigDecimalOps(self: BigDecimal.type) {
    val Zero: BigDecimal = BigDecimal("0.0")
  }

  implicit class ClockOps(self: Clock) {
    def now(): OffsetDateTime =
      OffsetDateTime.ofInstant(self.instant(), ZoneOffset.UTC)
  }

  sealed trait MachineCommand extends JsonSerializable {
    val replyTo: ActorRef[MachineReply]
  }
  final case class CreateMachineCommand(name: MachineName, replyTo: ActorRef[MachineReply]) extends MachineCommand
  final case class ChangeMachineStatusCommand(currentStatus: MachineStatus, replyTo: ActorRef[MachineReply]) extends MachineCommand

  sealed trait MachineEvent extends JsonSerializable
  final case class MachineCreated(name: MachineName, createdAt: OffsetDateTime) extends MachineEvent
  final case class MachineStatusChanged(currentStatus: MachineStatus, changedAt: OffsetDateTime) extends MachineEvent

  sealed trait MachineReply extends JsonSerializable
  object MachineReply {
    case object Success extends MachineReply
    case object Failure extends MachineReply
  }

  sealed trait MachineStatus extends EnumEntry
  object MachineStatus extends Enum[MachineStatus] {
    override def values: immutable.IndexedSeq[MachineStatus] = findValues

    val default = Off

    case object Standby extends MachineStatus
    case object Working extends MachineStatus
    case object Off extends MachineStatus
  }
  final case class MachineId(value: String)
  final case class MachineName(value: String)
  final case class CurrentValues(value: Map[MachineStatus, BigDecimal]) {
    def currentForStatus(status: MachineStatus): BigDecimal =
      value.getOrElse(status, BigDecimal.Zero)
  }

  sealed trait MachineState
  case object UninitializedMachine extends MachineState {
    def initialize(name: MachineName): InitializedMachine =
      InitializedMachine(name, MachineStatus.default)
  }
  final case class InitializedMachine(name: MachineName, currentStatus: MachineStatus) extends MachineState {
    def withStatus(newStatus: MachineStatus): InitializedMachine =
      copy(currentStatus = newStatus)
  }
  object MachineState {
    val empty: MachineState = UninitializedMachine
  }

  def apply(persistenceId: PersistenceId, clock: Clock, projectionTag: String): Behavior[MachineCommand] =
    EventSourcedBehavior(persistenceId = persistenceId, emptyState = MachineState.empty, commandHandler = CommandHandler(clock), eventHandler = EventHandler()).withTagger(_ => Set(projectionTag))

  private object CommandHandler {

    private val log = LoggerFactory.getLogger(getClass)

    def apply(clock: Clock): (MachineState, MachineCommand) => Effect[MachineEvent, MachineState] =
      (state, command) => {
        log.info(s"handling command $command")
        (state, command) match {
          case (UninitializedMachine, CreateMachineCommand(name, replyTo)) => createMachine(name, replyTo, clock)
          case (InitializedMachine(_, currentStatus), ChangeMachineStatusCommand(newStatus, replyTo)) => changeMachineStatus(currentStatus, newStatus, clock, replyTo)
          case (_, otherCommand) => unhandledCommand(otherCommand, state)
        }
      }

    private def createMachine(name: Machine.MachineName, replyTo: ActorRef[MachineReply], clock: Clock): ReplyEffect[MachineEvent, MachineState] =
      Effect.persist(MachineCreated(name, clock.now())).thenReply(replyTo)(_ => Machine.MachineReply.Success)

    private def changeMachineStatus(currentStatus: MachineStatus, newStatus: MachineStatus, clock: Clock, replyTo: ActorRef[MachineReply]): ReplyEffect[MachineEvent, MachineState] =
      if (currentStatus != newStatus) {
        log.info(s"changing status to $newStatus")
        Effect.persist(MachineStatusChanged(newStatus, clock.now())).thenReply(replyTo)(_ => Machine.MachineReply.Success)
      } else {
        log.info(s"current status is already $newStatus - ignoring command")
        Effect.reply(replyTo)(Machine.MachineReply.Success)
      }

    private def unhandledCommand(command: Machine.MachineCommand, state: MachineState): ReplyEffect[MachineEvent, MachineState] = {
      log.warn(s"unable to handle command ${command.getClass.getSimpleName} in state ${state.getClass.getSimpleName}")
      Effect.reply(command.replyTo)(Machine.MachineReply.Failure)
    }
  }

  private object EventHandler {

    private val log = LoggerFactory.getLogger(getClass)

    def apply(): (MachineState, MachineEvent) => MachineState =
      (state, event) => {
        log.info(s"handling event $event")
        state match {
          case UninitializedMachine => whenUninitialised(event)
          case initializedMachine: InitializedMachine => whenInitialized(initializedMachine, event)
        }
      }

    private def whenUninitialised(event: Machine.MachineEvent): MachineState =
      event match {
        case MachineCreated(name, _) => UninitializedMachine.initialize(name)
        case _ => unhandledEvent(UninitializedMachine, event)
      }

    private def whenInitialized(machine: Machine.InitializedMachine, event: Machine.MachineEvent): MachineState =
      event match {
        case MachineStatusChanged(newStatus, _) => machine.withStatus(newStatus)
        case _ => unhandledEvent(machine, event)
      }

    private def unhandledEvent(state: MachineState, event: MachineEvent): MachineState =
      throw new IllegalStateException(s"Unable to handle event '$event' in state '$state'")
  }
}

object MachineShardingRegion {

  val typeKey: EntityTypeKey[Machine.MachineCommand] = EntityTypeKey[Machine.MachineCommand]("machine")

  def init(system: ActorSystem[_], clock: Clock): ActorRef[ShardingEnvelope[Machine.MachineCommand]] =
    ClusterSharding(system).init(Entity(typeKey = typeKey) { entityContext =>
      val i = math.abs(entityContext.entityId.hashCode % DigitalTwinProjections.tags.size)
      val selectedTag = DigitalTwinProjections.tags(i)
      Machine(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId), clock, selectedTag)
    })
}
