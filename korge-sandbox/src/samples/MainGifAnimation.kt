package samples

import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.view.animation.*
import korlibs.image.format.*

class MainGifAnimation : Scene() {
    override suspend fun SContainer.sceneMain() {
        val imageData = korlibs.io.file.std.resourcesVfs["200.gif"]
            .readImageData(korlibs.image.format.GIF.toProps())
            //.packInMutableAtlas(MutableAtlasUnit())
        imageAnimationView(imageData.defaultAnimation)
        //image(imageData.mainBitmap)
    }
}
