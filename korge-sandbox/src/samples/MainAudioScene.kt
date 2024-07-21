package samples

import korlibs.audio.sound.*
import korlibs.io.file.std.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import kotlin.time.Duration.Companion.seconds

class MainAudioScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        uiVerticalStack(width = 640.0) {
            var lastChannel: SoundChannel? = null
            val len = uiText("Length: 0")
            suspend fun play(file: String, music: Boolean) {
                val vfs = resourcesVfs[file]
                lastChannel = if (music) vfs.readMusic().play() else vfs.readSound().play()
                len.text = "Length: ${lastChannel?.total}"
            }
            fun seekRatio(ratio: Double) {
                val computed = lastChannel?.total?.times(ratio) ?: if (ratio >= 1.0) 10000.seconds else 0.seconds
                lastChannel?.current = lastChannel?.total?.times(ratio) ?: if (ratio >= 1.0) 10000.seconds else 0.seconds
                len.text = "Length: ${lastChannel?.total}, Computed: $computed, Current: ${lastChannel?.current}"
            }

            uiButton(label = "Small MP3 Sound") { onClickSuspend { play("sounds/mp3.mp3", music = false) } }
            uiButton(label = "Small MP3 Music") { onClickSuspend { play("sounds/mp3.mp3", music = true) } }
            uiButton(label = "Small MP3 AudioData.toSound()") { onClickSuspend { lastChannel = resourcesVfs["sounds/mp3.mp3"].readAudioData().toSound().play() } }
            uiButton(label = "Small WAV Sound") { onClickSuspend { play("sounds/wav.wav", music = false) } }
            uiButton(label = "Small WAV Music") { onClickSuspend { play("sounds/wav.wav", music = true) } }
            uiButton(label = "Long MP3 Sound") { onClickSuspend { play("sounds/Snowland.mp3", music = false) } }
            uiButton(label = "Long MP3 Music") { onClickSuspend { play("sounds/Snowland.mp3", music = true) } }
            uiHorizontalStack {
                uiButton(label = "Seek Start") { onClickSuspend { seekRatio(0.001) } }
                uiButton(label = "Seek 0.25") { onClickSuspend { seekRatio(0.25) } }
                uiButton(label = "Seek Middle") { onClickSuspend { seekRatio(0.5) } }
                uiButton(label = "Seek 0.75") { onClickSuspend { seekRatio(0.75) } }
                uiButton(label = "Seek End") { onClickSuspend { seekRatio(1.0) } }
            }
            //uiButton(label = "OGG Sound") { onClick { resourcesVfs["sounds/ogg.ogg"].readSound().play() } }
            //uiButton(label = "OGG Music") { onClick { resourcesVfs["sounds/ogg.ogg"].readMusic().play() } }
        }

        try {
            println("TRYING: coroutineContext=$coroutineContext")
            //delay(1.seconds)
            println("BYTES: " + resourcesVfs["sounds/mp3.mp3"].readBytes().size)
            //localVfs("/tmp/demo.mp3").writeBytes()
            //localVfs("/tmp/demo.wav").writeBytes(WAV.encodeToByteArray(resourcesVfs["sounds/mp3.mp3"].readAudioData()))
            //println(resourcesVfs["sounds/mp3.mp3"].readAudioData().encodeToFile())
        } catch (e: Throwable)  {
            println("!!!!!!!!!!!! ERROR")
            println(coroutineContext)
            e.printStackTrace()
        }
    }
}


