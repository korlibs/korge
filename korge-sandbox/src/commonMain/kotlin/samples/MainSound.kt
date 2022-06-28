package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korau.sound.*
import com.soywiz.korio.file.std.*

class MainSound : Scene() {
    override suspend fun Container.sceneMain() {
        val music = resourcesVfs["sounds/Snowland.mp3"].readMusic()
//        val music = resourcesVfs["sounds/Deus_Ex_Tempus.ogg"].readMusic()
        val channel = music.play()
        channel.volume = 1.0
    }
}
