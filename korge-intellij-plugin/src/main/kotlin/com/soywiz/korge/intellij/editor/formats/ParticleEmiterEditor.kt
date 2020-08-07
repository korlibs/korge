package com.soywiz.korge.intellij.editor.formats

import com.soywiz.korev.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.component.docking.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.intellij.editor.*
import com.soywiz.korge.particle.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korma.geom.*
import kotlin.coroutines.*
import kotlin.math.*

suspend fun particleEmiterEditor(file: VfsFile): Module {
    val particle = file.readParticleEmitter()
    var doSave = false
    val uiContext = EmptyCoroutineContext

    return createModule {
        views.currentVfs = file.parent

        views.debugSavedHandlers.add {
            doSave = true
        }

        sceneView.addUpdater {
            if (doSave) {
                println("SAVED!")
                doSave = false
                launchImmediately(uiContext) {
                    file.writeParticleEmitter(particle)
                }
            }
        }

        val particleEmitterView = particle.create(0.0, 0.0)
        sceneView += particleEmitterView.dockedTo(Anchor.MIDDLE_CENTER)
        views.debugHightlightView(particleEmitterView)
    }
}
