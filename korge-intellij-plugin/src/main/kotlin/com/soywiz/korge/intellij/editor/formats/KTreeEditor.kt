package com.soywiz.korge.intellij.editor.formats

import com.soywiz.kds.iterators.*
import com.soywiz.korge.intellij.editor.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.xml.*

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
