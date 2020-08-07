package com.soywiz.korge.intellij.editor.formats

import com.soywiz.korge.animate.*
import com.soywiz.korge.animate.serialization.readAni
import com.soywiz.korge.awt.*
import com.soywiz.korge.ext.swf.SWFExportConfig
import com.soywiz.korge.ext.swf.readSWF
import com.soywiz.korge.intellij.editor.*
import com.soywiz.korge.intellij.editor.util.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.extensionLC

suspend fun swfAnimationEditor(file: VfsFile): Module {
    val context = AnLibrary.Context()
    val animationLibrary = when (file.extensionLC) {
        "swf" -> file.readSWF(
            context, defaultConfig = SWFExportConfig(
            mipmaps = false,
            antialiasing = true,
            rasterizerMethod = com.soywiz.korim.vector.ShapeRasterizerMethod.X4,
            exportScale = 1.0,
            exportPaths = false,
            atlasPacking = false
        )
        )
        "ani" -> file.readAni(context)
        else -> TODO()
    }

    return createModule {
        views = this.views

        val mainTimeline = animationLibrary.createMainTimeLine()
        val container = FixedSizeContainer(animationLibrary.width.toDouble(), animationLibrary.height.toDouble()).also {
            it.addChild(mainTimeline)
        }
        sceneView += container
        views.debugHightlightView(mainTimeline)
        container.repositionOnResize(this.views)
    }
}
