package samples

import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.klock.toTimeString
import com.soywiz.korau.module.new.readMOD
import com.soywiz.korau.sound.infinitePlaybackTimes
import com.soywiz.korev.Key
import com.soywiz.korge.input.keys
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.text
import com.soywiz.korio.file.std.resourcesVfs

class MainXM : Scene() {
    override suspend fun SContainer.sceneMain() {
        val sound = resourcesVfs["GUITAROU.MOD"].readMOD()
        val channel = sound.play(times = infinitePlaybackTimes)
        //val channel = sound.play(times = 2.playbackTimes)
        val timer = text("time: -")
        addUpdater {
            timer.text = "time: ${channel.current.toTimeString()}/${channel.total.toTimeString()}"
        }
        keys {
            down(Key.ENTER) {
                channel.current = 0.milliseconds
            }
            down(Key.LEFT) {
                channel.current -= if (it.shift) 10.seconds else 1.seconds
            }
            down(Key.RIGHT) {
                channel.current += if (it.shift) 10.seconds else 1.seconds
            }
        }
    }
}
