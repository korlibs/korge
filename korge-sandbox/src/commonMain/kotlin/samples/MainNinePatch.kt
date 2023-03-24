package samples

import korlibs.memory.clamp
import korlibs.korge.input.mouse
import korlibs.korge.scene.ScaledScene
import korlibs.korge.scene.Scene
import korlibs.korge.view.SContainer
import korlibs.korge.view.ninePatch
import korlibs.korge.view.position
import korlibs.image.bitmap.readNinePatch
import korlibs.io.file.std.resourcesVfs

//class MainNinePatch : ScaledScene(512, 512) {
class MainNinePatch : Scene() {
    override suspend fun SContainer.sceneMain() {
        val ninePath = resourcesVfs["image.9.png"].readNinePatch()

        val np = ninePatch(ninePath, 320.0, 32.0) {
            position(100, 100)
        }
        np.mouse {
            moveAnywhere {
                np.width = it.currentPosLocal.xD.clamp(16.0, width)
                np.height = it.currentPosLocal.yD.clamp(16.0, height)
            }
        }
    }
}
