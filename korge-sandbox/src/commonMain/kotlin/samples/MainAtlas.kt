package samples

import com.soywiz.korge.Korge
import com.soywiz.korge.scene.ScaledScene
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.container
import com.soywiz.korge.view.image
import com.soywiz.korge.view.position
import com.soywiz.korge.view.text
import com.soywiz.korim.atlas.MutableAtlasUnit
import com.soywiz.korim.atlas.readAtlas
import com.soywiz.korim.format.readBitmapSlice
import com.soywiz.korio.file.std.resourcesVfs

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
