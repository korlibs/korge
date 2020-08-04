package com.soywiz.korge.intellij.editor.formats

import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korge.input.*
import com.soywiz.korge.intellij.editor.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korma.geom.*

suspend fun ktreeEditor(file: VfsFile): KorgeBaseKorgeFileEditor.EditorModule {
    var save = false

    val viewTree = file.readXml().ktreeToViewTree()

    return createModule {
        val views = this.views
        val stage = views.stage
        // Dirty hack
        views.stage.removeChildren()

        if (viewTree is Container) {
            viewTree.children.toList().fastForEach {
                //println("ADDING: $it")
                stage.addChild(it)
            }
        }

        views.debugSavedHandlers.add {
            save = true
        }
        views.debugHightlightView(stage)

        var pressing = false
        var selectedView: View? = null
        var startSelectedViewPos = Point()
        var startSelectedMousePos = Point()
        stage.mouse {
            down {
                pressing = true
                val view2 = stage.hitTest(it.lastPosStage)
                if (view2 !== stage) {
                    views.debugHightlightView(view2)
                    selectedView = view2
                    if (view2 != null) {
                        startSelectedViewPos.setTo(view2.globalX, view2.globalY)
                        startSelectedMousePos.setTo(views.globalMouseX, views.globalMouseY)
                    }
                } else {
                    selectedView = null
                    views.debugHightlightView(selectedView)
                }
                //println("DOWN ON ${it.view}, $view2, ${it.lastPosStage}")
            }
            up {
                pressing = false
            }
            onMoveAnywhere {
                val view = selectedView
                if (pressing && view != null) {
                    val dx = views.globalMouseX - startSelectedMousePos.x
                    val dy = views.globalMouseY - startSelectedMousePos.y
                    view.globalX = (startSelectedViewPos.x + dx).nearestAlignedTo(16.0)
                    view.globalY = (startSelectedViewPos.y + dy).nearestAlignedTo(16.0)
                    //startSelectedViewPos.setTo(view2.globalX, view2.globalY)
                }
            }
        }

        stage.addUpdater {
            if (save) {
                save = false
                launchImmediately {
                    file.writeString(stage.viewTreeToKTree().toOuterXmlIndented().toString())
                }
            }
        }
    }
}
