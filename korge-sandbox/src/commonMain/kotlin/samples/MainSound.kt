package samples

import com.soywiz.klock.seconds
import com.soywiz.korau.format.AudioDecodingProps
import com.soywiz.korau.format.mp3.javamp3.JavaMp3AudioFormat
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korau.sound.*
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korio.async.delay
import com.soywiz.korio.file.std.*

class MainSound : Scene() {
    override suspend fun SContainer.sceneMain() {

        //nativeSoundProvider.audioFormats.registerFirst(JavaMp3AudioFormat())

        val music = resourcesVfs["sounds/Snowland.mp3"].readMusic()
        //val music = resourcesVfs["sounds/click.wav"].readSound()
        //val music = resourcesVfs["sounds/click.wav"].readMusic()

        val channel = music.play(times = infinitePlaybackTimes)
        delay(0.5.seconds)
        println(channel.current)
        delay(0.5.seconds)
        println(channel.current)
        channel.current = 0.seconds

        //val channel = music.play(times = 2.playbackTimes)
        //channel.volume = 0.1
    }
}
