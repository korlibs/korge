package samples

import korlibs.korge.scene.Scene
import korlibs.korge.view.SContainer
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.image.atlas.Atlas
import korlibs.image.atlas.readAtlas
import korlibs.image.bitmap.*
import korlibs.image.color.RGBA
import korlibs.image.format.ImageOrientation
import korlibs.io.file.std.resourcesVfs
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
                    1 -> atlas1.sliceWithSize(0, 0, 60, 50, "a$i").virtFrame(25, 20, 100, 100)
                    2 -> atlas1.sliceWithSize(0, 50, 60, 50, "a$i").virtFrame(25, 20, 100, 100)
                    3 -> atlas2.sliceWithSize(0, 0, 60, 50, "a$i").virtFrame(25, 20, 100, 100)
                    4 -> atlas2.sliceWithSize(0, 50, 60, 50, "a$i").virtFrame(25, 20, 100, 100)
                    5 -> atlas3.sliceWithSize(10, 0, 10, 100, "a$i").virtFrame(0, 45, 100, 100)
                    else -> atlas3.sliceWithSize(0, 0, 10, 100, "a$i").rotatedLeft().virtFrame(45, 0, 100, 100)
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
//
        generateImages(resourcesVfs["atlastest/atlas-test.json"].readAtlas(), ".png")
//
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
