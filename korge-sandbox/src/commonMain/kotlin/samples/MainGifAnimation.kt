package samples

import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.animation.*
import com.soywiz.korim.format.*

class MainGifAnimation : Scene() {
    override suspend fun SContainer.sceneMain() {
        val imageData = com.soywiz.korio.file.std.resourcesVfs["200.gif"]
            .readImageData(com.soywiz.korim.format.GIF.toProps())
            //.packInMutableAtlas(MutableAtlasUnit())
        imageAnimationView(imageData.defaultAnimation)
        //image(imageData.mainBitmap)
    }
}
