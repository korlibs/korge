package samples.pong

import korlibs.korge.scene.ScaledScene
import korlibs.korge.scene.sceneContainer
import korlibs.korge.view.SContainer

class MainPong : ScaledScene(800, 600) {
    override suspend fun SContainer.sceneMain() {
        injector.root.mapPrototype { MenuScene() }
        injector.root.mapPrototype { PlayScene() }
        sceneContainer().changeTo<MenuScene>()
    }
}
