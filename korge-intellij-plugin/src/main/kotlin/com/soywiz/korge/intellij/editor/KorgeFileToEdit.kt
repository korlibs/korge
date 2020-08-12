package com.soywiz.korge.intellij.editor

import com.intellij.openapi.command.*
import com.intellij.openapi.command.undo.*
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.util.*

data class KorgeFileToEdit(val originalFile: VirtualFile, val project: Project) : BaseKorgeFileToEdit(originalFile.toTextualVfs()) {
    val ref = DocumentReferenceManager.getInstance().create(originalFile)
    val doc = ref.document
    var lastSavedText = ""

    val documentListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            val newText = event.document.text
            if (newText != lastSavedText) {
                lastSavedText = newText
                //println("documentChanged")
                onChanged(newText)
            } else {
                //println("documentUnchanged")
            }
        }
    }

    init {
        doc?.addDocumentListener(documentListener)
    }

    var n = 0

    fun dispose() {
        doc?.removeDocumentListener(documentListener)
    }

    override fun save(text: String, message: String) {
        val oldText = doc?.text ?: ""
        if (oldText != text) {
            lastSavedText = text
            clearWriteActionNoWait()
            queueWriteActionNoWait {
                val id = n++
                CommandProcessor.getInstance().executeCommand(project, Runnable {
                    /*
                    val action = object : UndoableAction {
                        override fun undo() {
                            //doc.setText(oldText)
                            //load(oldText)
                        }
                        override fun redo() {
                            //doc.setText(text)
                            //load(text)
                        }
                        override fun getAffectedDocuments() = arrayOf(ref)
                        override fun isGlobal(): Boolean = false
                    }
                    UndoManager.getInstance(project).undoableActionPerformed(action)
                    */

                    doc?.setText(text)
                }, message, "korge$id", doc)
            }
        }
    }
}
