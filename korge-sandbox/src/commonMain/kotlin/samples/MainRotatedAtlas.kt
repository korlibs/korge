package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.image
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.xy
import com.soywiz.korim.atlas.Atlas
import com.soywiz.korim.atlas.readAtlas
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.BitmapSlice
import com.soywiz.korim.bitmap.asBitmapSlice
import com.soywiz.korim.bitmap.virtFrame
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.ImageOrientation
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.math.max
import kotlin.native.concurrent.ThreadLocal

class MainRotatedAtlas : Scene() {
    override suspend fun SContainer.sceneMain() {
        var y = 0

        fun generateImages(atlas: Atlas, ext: String = "") {
            var x = 0
            var maxY = 0
            for (i in 1..7) {
                val bmp = atlas["$i$ext"]
                solidRect(bmp.frameWidth, bmp.frameHeight, BG.color).xy(x, y)
                image(bmp).xy(x, y).also {
                    x += it.frameWidth.toInt()
                    maxY = max(maxY, it.frameHeight.toInt())
                }
            }
            y += maxY
        }

        fun generateImages(atlas: Atlas) {
            var x = 0
            val atlas1 = atlas["atlas1"]
            val atlas2 = atlas["atlas2"]
            val atlas3 = atlas["atlas3"]
            for (i in 1..6) {
                solidRect(100, 100, BG.color).xy(x, y)
                image(when(i) {
                    1 -> atlas1.sliceWithSize(0, 0, 50, 60, "a$i").asBitmapSlice<Bitmap>().virtFrame(25, 20, 100, 100)
                    2 -> atlas1.sliceWithSize(50, 0, 50, 60, "a$i").asBitmapSlice<Bitmap>().virtFrame(25, 20, 100, 100)
                    3 -> atlas2.sliceWithSize(0, 0, 50, 60, "a$i").asBitmapSlice<Bitmap>().virtFrame(25, 20, 100, 100)
                    4 -> atlas2.sliceWithSize(50, 0, 50, 60, "a$i").asBitmapSlice<Bitmap>().virtFrame(25, 20, 100, 100)
                    5 -> atlas3.sliceWithSize(0, 0, 100, 10, "a$i").asBitmapSlice<Bitmap>().virtFrame(0, 45, 100, 100)
                    else -> (atlas3.sliceWithSize(0, 10, 10, 100, "a$i", ImageOrientation.ROTATE_270) as BitmapSlice<Bitmap>).virtFrame(45, 0, 100, 100)
                }).xy(x, y).also {
                    if (i == 3) {
                        x = 0
                        y += 100
                    } else {
                        x += it.frameWidth.toInt()
                    }
                }
            }
        }

        generateImages(resourcesVfs["atlastest/atlas-test.xml"].readAtlas(), "")

        generateImages(resourcesVfs["atlastest/atlas-test-spine.atlas.txt"].readAtlas(), "")

        generateImages(resourcesVfs["atlastest/atlas-test.json"].readAtlas(), ".png")

        generateImages(resourcesVfs["atlastest/atlas.xml"].readAtlas())
    }
}

class BG {
    @ThreadLocal
    companion object {
        private val colAr = arrayOf(RGBA(240, 240, 240), RGBA(220, 220, 220))
        private var cur = true
        val color: RGBA
            get() {
                cur = !cur
                return colAr[if (cur) 1 else 0]
            }
    }
}

