package samples

import korlibs.time.seconds
import korlibs.korge.scene.Scene
import korlibs.korge.time.timeout
import korlibs.korge.view.SContainer
import korlibs.korge.view.text
import korlibs.korge.view.xy

class MainTextureIssue : Scene() {
    override suspend fun SContainer.sceneMain() {
        // Press F7 after 1 + 0.3 seconds (so the texture GC has been executed), this will trigger a new program creation

        text("HELLO WORLD!").also {
            timeout(0.3.seconds) { it.removeFromParent() }
        }

        val N = 1
        //val N = 3
        for (y in 0 until N) {
            for (x in 0 until N) {
                text("($x, $y)").xy(100 + x * 128, 100 + y * 16)
            }
        }
    }
}
