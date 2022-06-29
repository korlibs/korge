package samples

import com.soywiz.korau.format.AudioDecodingProps
import com.soywiz.korau.format.mp3.javamp3.JavaMp3AudioFormat
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korau.sound.*
import com.soywiz.korio.file.std.*

class MainSound : Scene() {
    override suspend fun SContainer.sceneMain() {

        nativeSoundProvider.audioFormats.registerFirst(JavaMp3AudioFormat())

        val music = resourcesVfs["sounds/Snowland.mp3"].readMusic()
        //val music = resourcesVfs["sounds/Snowland.mp3"].readSound()
        //val music = resourcesVfs["sounds/Deus_Ex_Tempus.ogg"].readMusic()
        //val channel = music.play(times = infinitePlaybackTimes)
        val channel = music.play()
        channel.volume = 1.0
    }
}
