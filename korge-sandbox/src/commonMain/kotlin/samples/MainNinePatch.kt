package samples

import korlibs.image.bitmap.*
import korlibs.io.file.std.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.memory.*

//class MainNinePatch : ScaledScene(512, 512) {
class MainNinePatch : Scene() {
    override suspend fun SContainer.sceneMain() {
        val ninePath = resourcesVfs["image.9.png"].readNinePatch()

        val np = ninePatch(ninePath, Size(320f, 32f)) {
            position(100, 100)
        }
        np.mouse {
            moveAnywhere {
                np.widthD = it.currentPosLocal.xD.clamp(16.0, widthD)
                np.heightD = it.currentPosLocal.yD.clamp(16.0, heightD)
            }
        }
    }
}
