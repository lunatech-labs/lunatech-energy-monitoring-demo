//
//  AnimationManager.swift
//  EnergyDemo
//
//  Created by Trevor Burton-McCreadie on 16/05/2023.
//

import SwiftUI
import RealityKit

class AnimationManager {
    
    private let service: DigitalTwinsService
    
    let entityId: String
    
    let anchor: RealityKit.Entity
    
    let animation: AnimationResource
    
    var animationController: AnimationPlaybackController?
    
    var timer: Timer?
    
    init(entityId id: String, _ anchor: RealityKit.Entity, _ animation: AnimationResource) {
        self.entityId = id
        self.service = DigitalTwinsService()
        self.anchor = anchor
        self.animation = animation
        
        Task {
            try! await service.createMachine(id, id)
        }
    }
    
    func start() {
        digitalTwinWorking()
        
        playAnimation()
        
        DispatchQueue.global(qos: .background).async {
            self.timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
                self.checkAnimation()
            }
            RunLoop.current.run()
        }
    }
    
    private func whenComplete() {
        if let timer = timer {
            timer.invalidate()
        }
        digitalTwinStopped()
        DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
            self.start()
        }
    }
    
    func checkAnimation() {
        if let controller = animationController {
            if (controller.isComplete) {
                print("animation complete")
                self.whenComplete()
            }
        } else {
            print("cannot manage animation, there's no animation to manage")
        }
    }
    
    func playAnimation() {
        self.animationController = anchor.playAnimation(named: "")
    }
    
    func digitalTwinWorking() {
        Task {
            try! await service.working(self.entityId)
        }
    }
    
    func digitalTwinStopped() {
        Task {
            try! await service.stopped(self.entityId)
        }
    }
}
