//
//  DigitalTwinsService.swift
//  EnergyDemo
//
//  Created by Trevor Burton-McCreadie on 12/05/2023.
//

import Foundation
import GRPC

class DigitalTwinsService {
    
    var digitalTwinClient: DigitalTwinServiceAsyncClient?
    let port: Int = 8080
    
    init() {
        let group = PlatformSupport.makeEventLoopGroup(loopCount: 1)
        
        do {
            let channel = try GRPCChannelPool.with(target: .host("192.168.1.101", port: self.port), transportSecurity: .plaintext, eventLoopGroup: group)
            self.digitalTwinClient = DigitalTwinServiceAsyncClient(channel: channel)
              print("grpc connection initialized")
            } catch {
              print("Couldn’t connect to gRPC server")
            }
    }
    
    func createMachine(_ id: String, _ name: String) async throws {
        print("Creating machine [id: \(id), name: \(name)]")
        if let client = self.digitalTwinClient {
            let request: CreateMachineRequest = .with {
                $0.machineID = id
                $0.machineName = name
            }
            
            do {
                let _ = try await client.createMachine(request)
            } catch {
                print("Failed to create machine [\(id): \(name)]: \(error)")
            }
        }
    }
    
    func working(_ id: String) async throws {
        try await changeMachineStatus(id, "Working")
    }
    
    func stopped(_ id: String) async throws {
        try await changeMachineStatus(id, "Off")
    }
    
    private func changeMachineStatus(_ id: String, _ status: String) async throws {
        if let client = self.digitalTwinClient {
            let request: ChangeMachineStatusRequest = .with {
                $0.machineID = id
                $0.newStatus = status
            }
            
            do {
                let _ = try await client.changeMachineStatus(request)
            } catch {
                print("Failed to change machine status: \(error)")
            }
        }
    }
    
    func listenToMachineStatusChanges() async throws {
        if let client = self.digitalTwinClient {
            for try await event in client.listenToMachineStatusChanges(Empty()) {
                print("received status change event \(event)")
            }
        } else {
            print("unable to listen to machine events, gRPC client is not defined")
        }
    }
}
