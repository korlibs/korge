package samples

import com.soywiz.klock.milliseconds
import com.soywiz.kmem.toUint8Buffer
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.scene.delay
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.image
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.bitmap.slice
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korvi.mpeg.JSMpegPlayer

class MainJSMpeg : Scene() {
    override suspend fun SContainer.sceneMain() {
        val image = image(Bitmaps.transparent)
        val data = resourcesVfs["blade-runner-2049-360p-5sec.mpeg1"].openInputStream()
        //val data = resourcesVfs["blade-runner-2049-1080p.ts.mpeg"].openInputStream()
        val player = JSMpegPlayer(coroutineContext)

        player.onDecodedVideoFrame.add {
            it.bitmap.lock {}

            //println("player.video.decodedTime=${player.video.decodedTime}, player.demuxer.currentTime=${player.demuxer.currentTime}, player.lastVideoTime=${player.lastVideoTime}")
            image.bitmap = it.bitmap.slice()
        }

        player.setStream(data)
        //addUpdater { player.frameSync() }

        launchImmediately {
            while (true) {
                player.frame()
            }
        }
    }
}
