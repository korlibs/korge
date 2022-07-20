package samples

import com.soywiz.klock.milliseconds
import com.soywiz.korev.addEventListener
import com.soywiz.korev.dispatch
import com.soywiz.korge.input.onClick
import com.soywiz.korge.lipsync.LipSyncEvent
import com.soywiz.korge.lipsync.readVoice
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.image
import com.soywiz.korge.view.position
import com.soywiz.korim.atlas.MutableAtlasUnit
import com.soywiz.korim.atlas.readAtlas
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.format.readBitmapSlice
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.std.resourcesVfs

class MainLipSync : Scene() {
    override suspend fun SContainer.sceneMain() {
        val atlas = MutableAtlasUnit()
        val lipsByChar = "ABCDEFGHX".associate { it to resourcesVfs["lips/lisa-$it.png"].readBitmapSlice(atlas = atlas) }
        val lips = image(lipsByChar['A']!!)
        val lips2 = image(lipsByChar['A']!!).position(400, 0)
        addOnEvent<LipSyncEvent> {
            println(it)
            if (it.name == "lisa") {
                lips2.bitmap = lipsByChar[it.lip]!!
            }
        }
        var playing = true
        fun play() = launchImmediately {
            fun handler(event: LipSyncEvent) {
                views.dispatch(event)
                lips.bitmap = lipsByChar[event.lip]!!
                playing = event.time > 0.milliseconds
            }

            resourcesVfs["001.voice.wav"].readVoice().play("lisa") { handler(it) }
            //resourcesVfs["002.voice.wav"].readVoice().play("lisa") { handler(it) }
            //resourcesVfs["003.voice.wav"].readVoice().play("lisa") { handler(it) }
            //resourcesVfs["004.voice.wav"].readVoice().play("lisa") { handler(it) }
            //resourcesVfs["simple.voice.mp3"].readVoice().play("lisa") { handler(it) }
        }

        onClick {
            if (!playing) play()
        }
        play()
    }
}
