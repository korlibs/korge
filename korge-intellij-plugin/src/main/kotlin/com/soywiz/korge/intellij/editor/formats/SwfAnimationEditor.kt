package com.soywiz.korge.intellij.editor.formats

import com.soywiz.korge.animate.serialization.readAni
import com.soywiz.korge.ext.swf.SWFExportConfig
import com.soywiz.korge.ext.swf.readSWF
import com.soywiz.korge.input.onClick
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.textButton
import com.soywiz.korge.view.*
import com.soywiz.korim.vector.ShapeRasterizerMethod
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.extensionLC

suspend fun Scene.swfAnimationEditor(file: VfsFile) {
    val animationLibrary = when (file.extensionLC) {
        "swf" -> file.readSWF(
            views, defaultConfig = SWFExportConfig(
            mipmaps = false,
            antialiasing = true,
            rasterizerMethod = com.soywiz.korim.vector.ShapeRasterizerMethod.X4,
            exportScale = 1.0,
            exportPaths = false
        )
        )
        "ani" -> file.readAni(views)
        else -> null
    }

    if (animationLibrary != null) {
        val container = sceneView.fixedSizeContainer(animationLibrary.width, animationLibrary.height) {
            this += animationLibrary.createMainTimeLine()
        }
        container.repositionOnResize(views)
    }

    sceneView.textButton(text = "Masks").apply {
        width = 80.0
        height = 24.0
        x = 0.0
        y = 0.0
        onClick {
            views.renderContext.masksEnabled = !views.renderContext.masksEnabled
        }
    }

    sceneView += Text("${file.baseName} : ${animationLibrary?.width}x${animationLibrary?.height}")
        .apply {
            x = 16.0
            y = 30.0
        }
}
