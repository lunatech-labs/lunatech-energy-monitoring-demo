package com.lunatech.energy.demo

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import com.lunatech.energy.demo.Machine.{ChangeMachineStatusCommand, CreateMachineCommand, MachineCommand, MachineId, MachineName, MachineReply, MachineStatus}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait MachineService {

  def createMachine(id: MachineId, name: MachineName): Future[Unit]

  def changeMachineStatus(id: MachineId, newStatus: MachineStatus): Future[Unit]
}

final class AkkaMachineService(system: ActorSystem[_]) extends MachineService {
  private implicit val sys: ActorSystem[_] = system
  private implicit val ec: ExecutionContext = system.executionContext
  private implicit val timeout: Timeout = Timeout(3.seconds)

  private val shard = ClusterSharding(system)

  override def createMachine(id: MachineId, name: MachineName): Future[Unit] =
    entityRefFor(id).ask[MachineReply](CreateMachineCommand(name, _)).map(_ => ())

  override def changeMachineStatus(id: MachineId, newStatus: MachineStatus): Future[Unit] =
    entityRefFor(id).ask[MachineReply](ChangeMachineStatusCommand(newStatus, _)).map(_ => ())

  private def entityRefFor(id: MachineId): EntityRef[MachineCommand] =
    shard.entityRefFor(MachineShardingRegion.typeKey, id.value)
}
