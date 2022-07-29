package samples

import com.soywiz.kmem.toUint8Buffer
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korvi.mpeg.JSMpegPlayer

class MainJSMpeg : Scene() {
    override suspend fun SContainer.sceneMain() {
        val data = resourcesVfs["blade-runner-2049-360p-5sec.mpeg1"].readAll()
        val player = JSMpegPlayer()
        player.write(data.toUint8Buffer())
        player.frame()
    }
}
