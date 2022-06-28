package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.anchor
import com.soywiz.korge.view.image
import com.soywiz.korge.view.rotation
import com.soywiz.korge.view.xy
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.BitmapSlice
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.RectangleInt
import com.soywiz.korma.geom.degrees

class MainTrimmedAtlas : Scene() {
    override suspend fun SContainer.sceneMain() {
        val bmp = BitmapSlice(
            Bitmap32(64, 64) { x, y -> Colors.PURPLE },
            RectangleInt(0, 0, 64, 64),
            null,
            RectangleInt(64, 64, 196, 196)
        )
        val image = image(bmp).anchor(0.5, 1.0).xy(200, 200).rotation(30.degrees)
        val image2 = image(bmp).anchor(0.0, 0.0).xy(200, 200)
        //addUpdater { image.rotation += 4.degrees }
    }
}
