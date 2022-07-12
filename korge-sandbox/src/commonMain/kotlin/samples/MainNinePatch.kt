package samples

import com.soywiz.kmem.clamp
import com.soywiz.korge.input.mouse
import com.soywiz.korge.scene.ScaledScene
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.ninePatch
import com.soywiz.korge.view.position
import com.soywiz.korim.bitmap.readNinePatch
import com.soywiz.korio.file.std.resourcesVfs

//class MainNinePatch : ScaledScene(512, 512) {
class MainNinePatch : Scene() {
    override suspend fun SContainer.sceneMain() {
        val ninePath = resourcesVfs["image.9.png"].readNinePatch()

        val np = ninePatch(ninePath, 320.0, 32.0) {
            position(100, 100)
        }
        np.mouse {
            moveAnywhere {
                np.width = it.currentPosLocal.x.clamp(16.0, width)
                np.height = it.currentPosLocal.y.clamp(16.0, height)
            }
        }
    }
}
