package com.soywiz.korge.intellij.editor.formats

import com.soywiz.korge.animate.*
import com.soywiz.korge.animate.serialization.readAni
import com.soywiz.korge.ext.swf.SWFExportConfig
import com.soywiz.korge.ext.swf.readSWF
import com.soywiz.korge.intellij.components.*
import com.soywiz.korge.intellij.editor.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.extensionLC

suspend fun swfAnimationEditor(file: VfsFile): KorgeBaseKorgeFileEditor.EditorModule {
    val context = AnLibrary.Context()
    var views: Views? = null
    val animationLibrary = when (file.extensionLC) {
        "swf" -> file.readSWF(
            context, defaultConfig = SWFExportConfig(
            mipmaps = false,
            antialiasing = true,
            //rasterizerMethod = com.soywiz.korim.vector.ShapeRasterizerMethod.X1,
            rasterizerMethod = com.soywiz.korim.vector.ShapeRasterizerMethod.X4,
            //rasterizerMethod = com.soywiz.korim.vector.ShapeRasterizerMethod.NONE,
            exportScale = 1.0,
            exportPaths = false,
            atlasPacking = false
        )
        )
        "ani" -> file.readAni(context)
        else -> TODO()
    }

    var repositionResult: RepositionResult? = null
    val container: Container = Container()
    val symbolNames = listOf("MainTimeLine") + animationLibrary.symbolsById.mapNotNull { it.name }
    val defaultSymbolName = when {
        "MainTimeLine" in symbolNames -> "MainTimeLine"
        else -> symbolNames.first()
    }

    val stateKeys = animationLibrary.mainTimeLineInfo.states.keys
    var mainTimeLine: AnMovieClip? = null
    var currentView: View? = null

    val symbolProperty = EditableEnumerableProperty("symbol", String::class, defaultSymbolName, symbolNames.toSet()).apply {
        this.onChange { symbolName ->
            views?.launchAsap {
                container.removeChildren()
                val childView: View = if (symbolName == "MainTimeLine") animationLibrary.createMainTimeLine() else (animationLibrary.create(symbolName) as View)
                currentView = childView
                val element = childView as AnElement
                if (element.symbol.id == 0) {
                    container.addChild(FixedSizeContainer(animationLibrary.width.toDouble(), animationLibrary.height.toDouble()).also { it.addChild(childView) })
                    mainTimeLine = childView as AnMovieClip
                } else {
                    container.addChild(childView)
                    mainTimeLine = null
                }
                repositionResult?.refreshBounds()
            }
        }
    }

    val gotoAndPlayProperty = EditableEnumerableProperty("gotoAndPlay", String::class, "__start", (listOf("__start") + stateKeys).toSet()).apply {
        this.onChange { frameName ->
            views?.launchAsap {
                //val state = animationLibrary.mainTimeLineInfo.states[frameName]
                //mainTimeLine?.timelineRunner?.currentStateName
                mainTimeLine?.play(frameName)
            }
        }
    }

    val gotoAndStopProperty = EditableEnumerableProperty("gotoAndStop", String::class, "__start", (listOf("__start") + stateKeys).toSet()).apply {
        this.onChange { frameName ->
            views?.launchAsap {
                //val state = animationLibrary.mainTimeLineInfo.states[frameName]
                //mainTimeLine?.timelineRunner?.currentStateName
                mainTimeLine?.playAndStop(frameName)
            }
        }
    }

    val gotoAndStopRatioProperty = EditableNumericProperty<Double>("stopAt", Double::class, 0.0, 0.0, 1.0).apply {
        this.onChange { ratio ->
            views?.launchAsap {
                //val state = animationLibrary.mainTimeLineInfo.states[frameName]
                //mainTimeLine?.timelineRunner?.currentStateName
                (currentView as? AnMovieClip?)?.stop()
                currentView?.ratio = ratio
                //mainTimeLine?.playAndStop()
            }
        }
    }

    return createModule(EditableNodeList {
        //add(EditableSection("Animation", animation1Property, animation2Property, blendingFactor, animationSpeed))
        add(EditableSection("SWF",
            InformativeProperty("name", file.baseName),
            InformativeProperty("size", "${animationLibrary.width}x${animationLibrary.height}"),
            container::speed.toEditableProperty(0.05, 10.0),
            symbolProperty,
            gotoAndPlayProperty,
            gotoAndStopProperty,
            gotoAndStopRatioProperty,
        ))
    }) {
        views = this.views

        //container = sceneView.fixedSizeContainer(animationLibrary.width, animationLibrary.height) {
        sceneView += container
        container.apply {
            fixedSizeContainer(animationLibrary.width, animationLibrary.height) {
                mainTimeLine = animationLibrary.createMainTimeLine()
                currentView = mainTimeLine
                this += mainTimeLine
            }
        }
        repositionResult = container.repositionOnResize(this.views)

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
