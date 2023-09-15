package samples

import korlibs.time.seconds
import korlibs.audio.format.AudioDecodingProps
import korlibs.korge.scene.Scene
import korlibs.korge.view.SContainer
import korlibs.audio.sound.*
import korlibs.korge.tween.get
import korlibs.korge.tween.tween
import korlibs.io.async.delay
import korlibs.io.file.std.*

class MainSound : Scene() {
    override suspend fun SContainer.sceneMain() {

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
