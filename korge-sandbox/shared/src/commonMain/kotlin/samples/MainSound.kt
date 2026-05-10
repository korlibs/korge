package samples

import korlibs.audio.sound.*
import korlibs.io.file.std.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.time.*

class MainSound : Scene() {
    override suspend fun SContainer.sceneMain() {
        //val music = resourcesVfs["sounds/Snowland.mp3"].readMusic()
        val music = resourcesVfs["sounds/Snowland.mp3"].readSound()
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
