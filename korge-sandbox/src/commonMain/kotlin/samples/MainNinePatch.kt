@file:OptIn(ExperimentalStdlibApi::class)

package samples

import korlibs.image.bitmap.*
import korlibs.io.file.std.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.math.geom.*

//class MainNinePatch : ScaledScene(512, 512) {
class MainNinePatch : Scene() {
    override suspend fun SContainer.sceneMain() {
        val ninePath = resourcesVfs["image.9.png"].readNinePatch()

        val np = ninePatch(ninePath, Size(320f, 32f)) {
            position(100, 100)
        }
        np.mouse {
            moveAnywhere {
                np.unscaledSize = it.currentPosLocal.clamp(Vector2(16f, 16f), Vector2(width, height)).toSize()
            }
        }
    }
}
