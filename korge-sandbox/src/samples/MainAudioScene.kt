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
            uiButton(label = "Small MP3 Sound") { onClickSuspend { resourcesVfs["sounds/mp3.mp3"].readSound().play() } }
            uiButton(label = "Small MP3 Music") { onClickSuspend { resourcesVfs["sounds/mp3.mp3"].readMusic().play() } }
            uiButton(label = "Small WAV Sound") { onClickSuspend { resourcesVfs["sounds/wav.wav"].readSound().play() } }
            uiButton(label = "Small WAV Music") { onClickSuspend { resourcesVfs["sounds/wav.wav"].readMusic().play() } }
            uiButton(label = "Long MP3 Sound") { onClickSuspend { resourcesVfs["sounds/Snowland.mp3"].readSound().play() } }
            uiButton(label = "Long MP3 Music") { onClickSuspend { resourcesVfs["sounds/Snowland.mp3"].readMusic().play() } }
            //uiButton(label = "OGG Sound") { onClick { resourcesVfs["sounds/ogg.ogg"].readSound().play() } }
            //uiButton(label = "OGG Music") { onClick { resourcesVfs["sounds/ogg.ogg"].readMusic().play() } }
        }
    }
}


