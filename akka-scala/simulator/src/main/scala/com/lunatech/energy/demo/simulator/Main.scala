package com.lunatech.energy.demo.simulator

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.scaladsl.{Concat, Sink, Source}
import com.lunatech.energy.demo._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Main {

  def main(args: Array[String]): Unit = {
    // Boot akka
    implicit val sys = ActorSystem("digital-twins-client")
    implicit val ec = sys.dispatcher

    // Configure the client by code:
    val clientSettings = GrpcClientSettings.connectToServiceAt("127.0.0.1", 8080).withTls(false)

    val client: DigitalTwinService = DigitalTwinServiceClient(clientSettings)

    def createMachine(): Unit = {
      sys.log.info("Performing request")
      val reply = client.createMachine(CreateMachineRequest("robot-arm", "Robot Arm"))
      reply.onComplete {
        case Success(msg) =>
          println(s"got reply: $msg")
        case Failure(e) =>
          println(s"Error createMachine: $e")
      }
    }

    def changeMachineStatus(newStatus: String): Future[Done] = {
      sys.log.info(s"Changing status of 'robot-arm' to '$newStatus''")
      client.changeMachineStatus(ChangeMachineStatusRequest("robot-arm", newStatus))
        .transformWith {
          case Success(msg) =>
            println(s"got reply: $msg")
            Future.successful(Done)
          case Failure(e) =>
            println(s"Error changeMachineStatus: $e")
            Future.successful(Done)
        }
    }

    val createMachineSource = Source.single(Done).map(_ => createMachine()).map(_ => Done)
    val changeMachineStatusSource = Source.repeat(Done).throttle(1, 2.seconds)
      .statefulMap(() => "Working")((state, _) => {
        val newState = if (state == "Working") {
          "Off"
        } else {
          "Working"
        }

        (newState, newState)
      }, _ => Some("Complete"))
      .mapAsync(1)(changeMachineStatus(_))

    val source = Source.combine(createMachineSource, changeMachineStatusSource)(Concat(_))

    source.to(Sink.foreach(println)).run()
  }
}
