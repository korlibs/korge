package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.animation.imageAnimationView
import com.soywiz.korim.format.readImageData

class MainGifAnimation : Scene() {
    override suspend fun SContainer.sceneMain() {
        val imageData = com.soywiz.korio.file.std.resourcesVfs["200.gif"].readImageData(com.soywiz.korim.format.GIF)
        imageAnimationView(imageData.defaultAnimation)
    }
}
