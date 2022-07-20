package samples.pong

import com.soywiz.korge.scene.ScaledScene
import com.soywiz.korge.scene.sceneContainer
import com.soywiz.korge.view.SContainer

class MainPong : ScaledScene(800, 600) {
    override suspend fun SContainer.sceneMain() {
        injector.root.mapPrototype { MenuScene() }
        injector.root.mapPrototype { PlayScene() }
        sceneContainer().changeTo<MenuScene>()
    }
}
