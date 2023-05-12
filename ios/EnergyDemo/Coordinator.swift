//
//  Coordinator.swift
//  EnergyDemo
//
//  Created by Trevor Burton-McCreadie on 16/05/2023.
//

import Foundation
import ARKit
import RealityKit

class Coordinator: NSObject {
    
    weak var view: ARView?
    
    @objc func handleTap(_ recognizer: UITapGestureRecognizer) {
        
        guard let view = self.view else { return }
        
        let tapLocation = recognizer.location(in: view)
        let results = view.raycast(from: tapLocation, allowing: .estimatedPlane, alignment: .horizontal)
        
        if let result = results.first {
            
            // ARAnchor - ARKit Framework
            // AnchorEntity - RealityKit Framework
            
            let anchorEntity = AnchorEntity(raycastResult: result)
            
            guard let modelEntity = try? Entity.load(named: "robot") else {
                fatalError("Unable to load robot model")
            }
            
            var transform = Transform()
            transform.translation.x = 0.0
            transform.translation.y = -5.0
            transform.translation.z = -15.0
            transform.scale.x = 0.1
            transform.scale.y = 0.1
            transform.scale.z = 0.1
            
            anchorEntity.addChild(modelEntity)
            
            view.scene.addAnchor(anchorEntity)
            
            modelEntity.move(to: transform, relativeTo: nil)
        }
    }
    
}
