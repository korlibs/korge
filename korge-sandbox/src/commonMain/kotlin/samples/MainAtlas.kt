package samples

import korlibs.korge.Korge
import korlibs.korge.scene.ScaledScene
import korlibs.korge.scene.Scene
import korlibs.korge.view.SContainer
import korlibs.korge.view.Stage
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.position
import korlibs.korge.view.text
import korlibs.image.atlas.MutableAtlasUnit
import korlibs.image.atlas.readAtlas
import korlibs.image.format.readBitmapSlice
import korlibs.io.file.std.resourcesVfs

class MainAtlas : ScaledScene(640, 480) {
    override suspend fun SContainer.sceneMain() {
        container {
            position(0, 0)
            text("On-the-fly atlas").position(0, 128)
            val atlas = MutableAtlasUnit()
            val korauTexture = resourcesVfs["logos/korau.png"].readBitmapSlice(atlas = atlas)
            val korimTexture = resourcesVfs["logos/korim.png"].readBitmapSlice(atlas = atlas)
            val korgeTexture = resourcesVfs["logos/korge.png"].readBitmapSlice(atlas = atlas)
            image(korauTexture).position(0, 0)
            image(korimTexture).position(64, 32)
            image(korgeTexture).position(128, 64)
            image(atlas.bitmap).position(256, 0)
        }

        container {
            position(0, 256)
            text("Compile-time atlas").position(0, 128)
            val logos = resourcesVfs["logos.atlas.json"].readAtlas()
            image(logos["korau.png"]).position(0, 0)
            image(logos["korim.png"]).position(64, 32)
            image(logos["korge.png"]).position(128, 64)
            image(logos.texture).position(256, 0)
        }
    }
}