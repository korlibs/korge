package samples

import com.soywiz.korau.module.new.readMOD
import com.soywiz.korau.module.readXMOld
import com.soywiz.korau.module.xm.XM
import com.soywiz.korau.module.xm.readXM
import com.soywiz.korau.sound.infinitePlaybackTimes
import com.soywiz.korau.sound.playAndWait
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korio.file.std.resourcesVfs

class MainXM : Scene() {
    override suspend fun SContainer.sceneMain() {
        val sound = resourcesVfs["GUITAROU.MOD"].readMOD()
        sound.playAndWait(times = infinitePlaybackTimes)

        /*
        val xm = resourcesVfs["sounds/poliamber.xm"].readXMOld()
        //val xm = resourcesVfs["sounds/_sunlight_.xm"].readXM()
        //val xm = resourcesVfs["sounds/transatlantic.xm"].readXM()
        //xm.load(resourcesVfs["sounds/poliamber.xm"].readBytes())
        //xm.load(resourcesVfs["sounds/transatlantic.xm"].readBytes())
        //xm.playAndWait()
        xm.createAudioStream().playAndWait()
        /*
        val ev = XM.AudioEvent(
            44100, 0.0, XM.AudioBuffer(arrayOf(FloatArray(8000), FloatArray(8000)))
        )
        xm.audio_cb(ev)
        println(ev.outputBuffer.channels[0].toList())
        println(ev)

         */
        //WAV.encodeToByteArray(xm.createAudioStream().toData()).writeToFile("/tmp/lol.wav")

        //xm.createAudioStream().playAndWait()

         */
    }
}
