package samples

import korlibs.image.bitmap.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.scene.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.time.*

class MainMipmaps : Scene() {
    override suspend fun SContainer.sceneMain() {
        val image = image(resourcesVfs["korge.png"].readBitmap().mipmaps())
        //image.program = BatchBuilder2D.PROGRAM_PRE_WRAP
        while (true) {
            tween(image::scaleAvg[0.01f], time = 3.seconds)
            tween(image::scaleAvg[0.2f], time = 1.seconds)
        }
    }
}
