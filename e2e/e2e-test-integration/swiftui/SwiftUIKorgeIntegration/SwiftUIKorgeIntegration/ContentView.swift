//
//  ContentView.swift
//  SwiftUIKorgeIntegration
//
//  Created by Carlos Ballesteros Velasco on 10/7/23.
//

import SwiftUI
import GameMain

struct ContentView: View {
    var body: some View {
        VStack {
            Image(systemName: "globe")
                .imageScale(.large)
                .foregroundColor(.accentColor)
            Text("Hello, world!")
            MyKorgeGameView()
            Text("Hello, world!")
            MyKorgeGameView()
        }
        .padding()
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}

struct MyKorgeGameView: UIViewRepresentable {
    func makeUIView(context: Context) -> UIView {
        let viewInfo = KorgeIosUIViewProvider().createViewInfo(scene: MyScene(), width: 600, height: 600)
        return viewInfo.view
    }
    
    func sizeThatFits(_ proposal: ProposedViewSize, uiView: UIView, context: Context) -> CGSize? {
        return CGSize(width: 200, height: 125)
    }
    
    func updateUIView(_ uiView: UIView, context: Context) {
    }
}
