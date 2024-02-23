package samples

import korlibs.audio.sound.*
import korlibs.io.file.std.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*

class MainAudioScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        uiVerticalStack(width = 640.0) {
            uiButton(label = "MP3 Sound") { onClick { resourcesVfs["sounds/mp3.mp3"].readSound().play() } }
            uiButton(label = "MP3 Music") { onClick { resourcesVfs["sounds/mp3.mp3"].readMusic().play() } }
            uiButton(label = "WAV Sound") { onClick { resourcesVfs["sounds/wav.wav"].readSound().play() } }
            uiButton(label = "WAV Music") { onClick { resourcesVfs["sounds/wav.wav"].readMusic().play() } }
            //uiButton(label = "OGG Sound") { onClick { resourcesVfs["sounds/ogg.ogg"].readSound().play() } }
            //uiButton(label = "OGG Music") { onClick { resourcesVfs["sounds/ogg.ogg"].readMusic().play() } }
        }
    }
}


