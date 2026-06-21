package samples

import korlibs.korge.scene.Scene
import korlibs.korge.view.SContainer
import korlibs.korge.view.animation.imageAnimationView
import korlibs.image.atlas.MutableAtlasUnit
import korlibs.image.format.ASE
import korlibs.image.format.ImageDecodingProps
import korlibs.image.format.readImageData
import korlibs.image.format.toProps
import korlibs.io.file.std.resourcesVfs

class MainAseprite : Scene() {
    override suspend fun SContainer.sceneMain() {
        val atlas = MutableAtlasUnit()
        val image = resourcesVfs["asepritetilemap.aseprite"].readImageData(ASE.toProps(), atlas = atlas)
        imageAnimationView(image.defaultAnimation)
    }
}
