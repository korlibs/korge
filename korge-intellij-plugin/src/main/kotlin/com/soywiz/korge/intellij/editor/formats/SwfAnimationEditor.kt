package com.soywiz.korge.intellij.editor.formats

import com.soywiz.korge.animate.*
import com.soywiz.korge.animate.serialization.readAni
import com.soywiz.korge.ext.swf.SWFExportConfig
import com.soywiz.korge.ext.swf.readSWF
import com.soywiz.korge.input.onClick
import com.soywiz.korge.intellij.components.*
import com.soywiz.korge.intellij.editor.*
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.textButton
import com.soywiz.korge.view.*
import com.soywiz.korim.vector.ShapeRasterizerMethod
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.extensionLC
import kotlin.coroutines.*

suspend fun swfAnimationEditor(file: VfsFile): KorgeBaseKorgeFileEditor.EditorModule {
    val context = AnLibrary.Context()
    val animationLibrary = when (file.extensionLC) {
        "swf" -> file.readSWF(
            context, defaultConfig = SWFExportConfig(
            mipmaps = false,
            antialiasing = true,
            rasterizerMethod = com.soywiz.korim.vector.ShapeRasterizerMethod.X4,
            exportScale = 1.0,
            exportPaths = false
        )
        )
        "ani" -> file.readAni(context)
        else -> TODO()
    }

    var repositionResult: RepositionResult? = null
    var container: Container? = null
    val symbolNames = animationLibrary.symbolsById.mapNotNull { it.name }
    val defaultSymbolName = when {
        "MainTimeLine" in symbolNames -> "MainTimeLine"
        else -> symbolNames.first()
    }

    val symbolProperty = EditableEnumerableProperty("symbol", String::class, defaultSymbolName, symbolNames.toSet()).apply {
        this.onChange { symbolName ->
            container?.removeChildren()
            val childView = animationLibrary.create(symbolName) as View
            if (symbolName == "MainTimeLine") {
                container?.addChild(FixedSizeContainer(animationLibrary.width.toDouble(), animationLibrary.height.toDouble()).also { it.addChild(childView) })
            } else {
                container?.addChild(childView)
            }
            repositionResult?.refreshBounds()
        }
    }

    return createModule(EditableNodeList {
        //add(EditableSection("Animation", animation1Property, animation2Property, blendingFactor, animationSpeed))
        add(EditableSection("SWF",
            InformativeProperty("name", file.baseName),
            InformativeProperty("size", "${animationLibrary?.width}x${animationLibrary?.height}"),
            symbolProperty,
        ))
    }) {

        //container = sceneView.fixedSizeContainer(animationLibrary.width, animationLibrary.height) {
        container = sceneView.container {
            fixedSizeContainer(animationLibrary.width, animationLibrary.height) {
                this += animationLibrary.createMainTimeLine()
            }
        }
        repositionResult = container!!.repositionOnResize(views)

        /*
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
         */
    }
}
