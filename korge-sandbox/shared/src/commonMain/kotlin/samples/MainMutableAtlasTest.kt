package samples

import korlibs.korge.scene.Scene
import korlibs.korge.view.SContainer
import korlibs.korge.view.image
import korlibs.korge.view.scale
import korlibs.korge.view.xy
import korlibs.image.atlas.MutableAtlasUnit
import korlibs.image.format.readBitmapSlice
import korlibs.image.format.readBitmapSliceWithOrientation
import korlibs.io.file.std.resourcesVfs

class MainMutableAtlasTest : Scene() {
    override suspend fun SContainer.sceneMain() {
        val atlas = MutableAtlasUnit(4096)
        image(resourcesVfs["Portrait_3.jpg"].readBitmapSlice(atlas = atlas)).scale(0.2)
        image(resourcesVfs["Portrait_3.jpg"].readBitmapSliceWithOrientation(atlas = atlas)).scale(0.2).xy(300, 0)
        image(atlas.bitmap).scale(0.2).xy(600, 0)
    }
}
