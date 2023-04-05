package samples

import korlibs.time.*
import korlibs.korge.scene.Scene
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.image.bitmap.*
import korlibs.image.format.*
import korlibs.io.file.std.*

class MainMipmaps : Scene() {
    override suspend fun SContainer.sceneMain() {
        val image = image(resourcesVfs["korge.png"].readBitmap().mipmaps())
        //image.program = BatchBuilder2D.PROGRAM_PRE_WRAP
        while (true) {
            tween(image::scale[0.01f], time = 3.seconds)
            tween(image::scale[0.2f], time = 1.seconds)
        }
    }
}
