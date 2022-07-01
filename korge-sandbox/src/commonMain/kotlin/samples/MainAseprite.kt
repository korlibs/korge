package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.animation.imageAnimationView
import com.soywiz.korim.atlas.MutableAtlasUnit
import com.soywiz.korim.format.ASE
import com.soywiz.korim.format.readImageData
import com.soywiz.korio.file.std.resourcesVfs

class MainAseprite : Scene() {
    override suspend fun SContainer.sceneMain() {
        val atlas = MutableAtlasUnit()
        val image = resourcesVfs["asepritetilemap.aseprite"].readImageData(ASE, atlas = atlas)
        imageAnimationView(image.defaultAnimation)
    }
}
