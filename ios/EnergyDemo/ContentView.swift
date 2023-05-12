//
//  ContentView.swift
//  EnergyDemo
//
//  Created by Trevor Burton-McCreadie on 12/05/2023.
//

import SwiftUI
import RealityKit

struct ContentView : View {
    var body: some View {
        ARViewContainer().edgesIgnoringSafeArea(.all)
    }
}

struct ARViewContainer: UIViewRepresentable {
    
    func makeUIView(context: Context) -> ARView {

        let arView = ARView(frame: .zero)

        loadRobot("robot1", arView)
        loadRobot("robot2", arView)
        loadRobot("robot3", arView)
        loadRobot("robot4", arView)
        
        return arView
    }
    
    func updateUIView(_ uiView: ARView, context: Context) {}
    
    private func loadRobot(_ image: String, _ arView: ARView) {
        guard let robot = try? Entity.load(named: "robot") else {
            fatalError("Unable to load robot model")
        }

        let anchor = AnchorEntity(.image(group: "AR Resources", name: image))
        anchor.addChild(robot)
        arView.scene.addAnchor(anchor)
        
        robot.position.z -= 1.9
        robot.position.x += 0.47
        
        let animation = anchor.availableAnimations.first

        if let animation = animation {
            AnimationManager(entityId: image, anchor, animation).start()
        }
    }
}

#if DEBUG
struct ContentView_Previews : PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
#endif
