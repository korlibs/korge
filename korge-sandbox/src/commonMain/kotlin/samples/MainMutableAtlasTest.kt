package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.image
import com.soywiz.korge.view.scale
import com.soywiz.korge.view.xy
import com.soywiz.korim.atlas.MutableAtlasUnit
import com.soywiz.korim.format.readBitmapSlice
import com.soywiz.korim.format.readBitmapSliceWithOrientation
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.degrees

class MainMutableAtlasTest : Scene() {
    override suspend fun Container.sceneMain() {
        val atlas = MutableAtlasUnit(4096)
        image(resourcesVfs["Portrait_3.jpg"].readBitmapSlice(atlas = atlas)).scale(0.2)
        image(resourcesVfs["Portrait_3.jpg"].readBitmapSliceWithOrientation(atlas = atlas)).scale(0.2).xy(300, 0)
        image(atlas.bitmap).scale(0.2).xy(600, 0)
    }
}
