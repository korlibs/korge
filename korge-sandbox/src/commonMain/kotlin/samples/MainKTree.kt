package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.ktree.readKTree
import com.soywiz.korio.file.std.resourcesVfs

class MainKTree : Scene() {
    override suspend fun SContainer.sceneMain() {
        addChild(resourcesVfs["scene.ktree"].readKTree(views))
    }
}
