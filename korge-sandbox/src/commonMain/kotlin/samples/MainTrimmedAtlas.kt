package samples

import korlibs.korge.scene.Scene
import korlibs.korge.view.SContainer
import korlibs.korge.view.anchor
import korlibs.korge.view.image
import korlibs.korge.view.rotation
import korlibs.korge.view.xy
import korlibs.image.bitmap.Bitmap32
import korlibs.image.bitmap.BitmapSlice
import korlibs.image.color.Colors
import korlibs.math.geom.*

class MainTrimmedAtlas : Scene() {
    override suspend fun SContainer.sceneMain() {
        val bmp = BitmapSlice(
            Bitmap32(64, 64) { x, y -> Colors.PURPLE },
            RectangleInt(0, 0, 64, 64),
            name = null,
        ).virtFrame(RectangleInt(64, 64, 196, 196))
        val image = image(bmp).anchor(0.5, 1.0).xy(200, 200).rotation(30.degrees)
        val image2 = image(bmp).anchor(0.0, 0.0).xy(200, 200)
        //addUpdater { image.rotation += 4.degrees }

        /**
         *     override val bmp: T,
         *     bounds: RectangleInt,
         *     name: String? = null,
         *     virtFrame: RectangleInt? = null,
         *     bmpCoords: BmpCoordsWithT<*> = BmpCoordsWithInstance(bmp, premultiplied = bmp.premultiplied),
         */
    }
}