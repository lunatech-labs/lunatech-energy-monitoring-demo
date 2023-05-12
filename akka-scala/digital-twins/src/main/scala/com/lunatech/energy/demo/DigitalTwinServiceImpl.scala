package com.lunatech.energy.demo

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import com.lunatech.energy.demo.Machine.{CurrentValues, MachineId, MachineName, MachineStatus}

import scala.concurrent.{ExecutionContext, Future}

class DigitalTwinServiceImpl(system: ActorSystem[_], machines: MachineService) extends DigitalTwinService {
  private implicit val ec: ExecutionContext = system.executionContext

  override def createMachine(in: CreateMachineRequest): Future[Empty] = {
    val machineId = MachineId(in.machineId)
    val machineName = MachineName(in.machineName)
    val currentValues = CurrentValues(Map.empty)
    machines.createMachine(machineId, machineName).map(_ => Empty.defaultInstance)
  }

  override def changeMachineStatus(in: ChangeMachineStatusRequest): Future[Empty] = {
    val id = MachineId(in.machineId)
    val newStatus = MachineStatus.withName(in.newStatus)
    machines.changeMachineStatus(id, newStatus).map(_ => Empty.defaultInstance)
  }

  override def listenToMachineStatusChanges(in: Empty): Source[MachineState, NotUsed] = ???
}
