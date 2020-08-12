package com.soywiz.korge.intellij.editor

import com.soywiz.korge.awt.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korinject.*

@Prototype
class EditorScene(
    val fileToEdit: KorgeFileToEdit,
    val blockToExecute: BlockToExecute,
    val viewsDebuggerComponent: ViewsDebuggerComponent
) : Scene() {
    override suspend fun Container.sceneMain() {
        val loading = text("Loading...", color = Colors.WHITE).apply {
            //format = Html.Format(align = Html.Alignment.CENTER)
            //x = views.virtualWidth * 0.5
            //y = views.virtualHeight * 0.5
            x = 16.0
            y = 16.0
        }

        //val uiFrameView = ui.koruiFrame {}
        //sceneView += uiFrameView
        //val frame = uiFrameView.frame

        delayFrame()

        try {
            blockToExecute.block(this@EditorScene)
        } catch (e: Throwable) {
            sceneView.text("Error: ${e.message}").centerOnStage()
            e.printStackTrace()
        }
        sceneView -= loading

        Unit
    }
}
