package com.soywiz.korge.intellij.editor.formats

import com.soywiz.klock.*
import com.soywiz.korau.sound.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.input.onClick
import com.soywiz.korge.intellij.editor.*
import com.soywiz.korge.lipsync.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.ui.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*

private suspend fun getLipTexture(char: Char) =
    runCatching { KorgeFileEditorProvider.pluginResurcesVfs["/com/soywiz/korge/intellij/lips/lisa-$char.png"].readBitmapSlice() }.getOrNull()
        ?: Bitmaps.transparent

suspend fun Scene.audioFileEditor(file: VfsFile) {
    var voice: Voice? = null
    val voiceName = "voice"

    if (file.baseName.contains("voice") || file.baseName.contains("lipsync")) {
        val wav = file.withExtension("wav")
        val mp3 = file.withExtension("mp3")
        val ogg = file.withExtension("ogg")
        val audios = listOf(wav, mp3, ogg)
        val audio = audios.firstOrNull { it.exists() }
        voice = audio?.readVoice()
        //audio?.readAudioData()?.play()

        //val classLoader = pluginClassLoader

        views.setVirtualSize(408 * 2, 334 * 2)

        val mouth = AnSimpleAnimation(
            10.milliseconds, mapOf(
            "A" to listOf(getLipTexture('A')),
            "B" to listOf(getLipTexture('B')),
            "C" to listOf(getLipTexture('C')),
            "D" to listOf(getLipTexture('D')),
            "E" to listOf(getLipTexture('E')),
            "F" to listOf(getLipTexture('F')),
            "G" to listOf(getLipTexture('G')),
            "H" to listOf(getLipTexture('H')),
            "X" to listOf(getLipTexture('X'))
        ), Anchor.MIDDLE_CENTER
        ).apply {
            x = views.virtualWidth * 0.5
            y = views.virtualHeight * 0.5
            addProp("lipsync", voiceName)
        }

        sceneView += mouth
    }

    var promise: Job? = null

    fun stopSound() {
        promise?.cancel()
        println("stopSound")
    }

    fun playSound() {
        println("playSound")
        promise?.cancel()
        promise = launchImmediately {
            if (voice != null) {
                voice?.play(voiceName, views)
            } else {
                file.readSound(streaming = true).play()
                //views.soundSystem.play(file.readNativeSoundOptimized())
            }
            Unit
        }
    }

    playSound()

    //sceneView.addEventListener<LipSyncEvent> { e ->
    //	mouth.tex = lips[e.lip] ?: views.transparentTexture
    //}

    sceneView.uiTextButton(text = "Replay").apply {
        width = 80.0
        height = 24.0
        x = 0.0
        y = 0.0
        onClick {
            playSound()
        }
    }

    sceneView.uiTextButton(text = "Stop").apply {
        width = 80.0
        height = 24.0
        x = 80.0
        y = 0.0
        onClick {
            stopSound()
        }
    }

}
