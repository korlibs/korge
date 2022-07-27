package samples

import com.soywiz.klock.measureTime
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.klock.toTimeString
import com.soywiz.korau.format.defaultAudioFormats
import com.soywiz.korau.mod.MOD
import com.soywiz.korau.mod.S3M
import com.soywiz.korau.mod.XM
import com.soywiz.korau.sound.infinitePlaybackTimes
import com.soywiz.korau.sound.readMusic
import com.soywiz.korev.Key
import com.soywiz.korge.input.keys
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.UIText
import com.soywiz.korge.ui.uiComboBox
import com.soywiz.korge.ui.uiText
import com.soywiz.korge.ui.uiVerticalStack
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.addUpdater
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.std.resourcesVfs

class MainXM : Scene() {
    override suspend fun SContainer.sceneMain() {
        measureTime({
            defaultAudioFormats.register(MOD, S3M, XM)
        }) {
            println("Registered sound module track formats in $it")
        }
        println(defaultAudioFormats.extensions)
        val soundsFolder = resourcesVfs["sounds"]
        val sound = measureTime({ soundsFolder["GUITAROU.MOD"].readMusic() }) {
        //val sound = measureTime({ soundsFolder["GUITAROU.MOD"].readSound() }) {
            println("Read music file in $it")
        }
        //val sound = resourcesVfs["sounds/_sunlight_.xm"].readXM()
        //val sound = resourcesVfs["sounds/12oz.s3m"].readS3M()
        //val sound = resourcesVfs["sounds/poliamber.xm"].readXM()
        //val sound = resourcesVfs["sounds/transatlantic.xm"].readXM()
        var channel = sound.play(times = infinitePlaybackTimes)
        //val channel = sound.play(times = 2.playbackTimes)
        lateinit var timer: UIText
        uiVerticalStack(width = 400.0) {
            uiComboBox(items = soundsFolder.listNames().filter { it.substringAfterLast('.').lowercase() in defaultAudioFormats.extensions }).also {
                it.onSelectionUpdate { box ->
                    launchImmediately {
                        channel.stop()
                        val sound = measureTime({ soundsFolder[box.selectedItem!!].readMusic() }) {
                            println("Read music file in $it")
                        }
                        channel = sound.play(times = infinitePlaybackTimes)
                    }
                }
            }
            timer = uiText("time: -")
        }
        addUpdater {
            timer.text = "time: ${channel.current.toTimeString()}/${channel.total.toTimeString()}"
        }
        keys {
            down(Key.ENTER) {
                channel.current = 0.milliseconds
            }
            down(Key.LEFT) {
                channel.current = (channel.current - if (it.shift) 10.seconds else 1.seconds) umod channel.total
            }
            down(Key.RIGHT) {
                channel.current += if (it.shift) 10.seconds else 1.seconds
            }
        }
    }
}
