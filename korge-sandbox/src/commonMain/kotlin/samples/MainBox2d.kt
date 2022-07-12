package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.ktree.readKTree
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korge.box2d.*

class MainBox2d : Scene() {
    override suspend fun SContainer.sceneMain() {
        registerBox2dSupportOnce()
        addChild(resourcesVfs["restitution.ktree"].readKTree(views))

    }
}

/*
suspend fun main() = Korge(width = 920, height = 720, quality = GameWindow.Quality.PERFORMANCE, title = "My Awesome Box2D Game!") {
    registerBox2dSupportOnce()
    addChild(resourcesVfs["restitution.ktree"].readKTree(views))
}

 */
