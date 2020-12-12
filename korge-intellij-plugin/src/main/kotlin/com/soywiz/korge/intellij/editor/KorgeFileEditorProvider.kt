package com.soywiz.korge.intellij.editor

import com.intellij.openapi.fileEditor.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.intellij.components.*
import com.soywiz.korge.intellij.editor.formats.*
import com.soywiz.korge.intellij.util.*
import com.soywiz.korge.resources.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.vector.format.*
import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.*
import kotlin.reflect.*

open class KorgeFileEditorProvider : com.intellij.openapi.fileEditor.FileEditorProvider, com.intellij.openapi.project.DumbAware {
	companion object {
		val pluginClassLoader: ClassLoader = KorgeFileEditorProvider::class.java.classLoader
		//val pluginResurcesVfs by lazy { resourcesVfs(pluginClassLoader) }
		val pluginResurcesVfs: VfsFile get() = resourcesVfs
	}

    var myPolicy: FileEditorPolicy = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR

    override fun accept(
        project: com.intellij.openapi.project.Project,
        virtualFile: com.intellij.openapi.vfs.VirtualFile
    ): Boolean {
        val name = virtualFile.name
        return when {
            ////////
            name.endsWith(".svg", ignoreCase = true) -> true
            name.endsWith(".pex", ignoreCase = true) -> true
            name.endsWith(".ktree", ignoreCase = true) -> true
            name.endsWith(".scml", ignoreCase = true) -> true
            name.endsWith("_ske.json", ignoreCase = true) -> true
            ////////
            name.endsWith(".swf", ignoreCase = true) -> true
            name.endsWith(".ani", ignoreCase = true) -> true
            name.endsWith(".voice.wav", ignoreCase = true) -> true
            name.endsWith(".voice.mp3", ignoreCase = true) -> true
            name.endsWith(".voice.ogg", ignoreCase = true) -> true
            name.endsWith(".voice.lipsync", ignoreCase = true) -> true
            name.endsWith(".wav", ignoreCase = true) -> true
            name.endsWith(".mp3", ignoreCase = true) -> true
            name.endsWith(".ogg", ignoreCase = true) -> true
            name.endsWith(".dbbin", ignoreCase = true) -> true
            name.endsWith(".skel", ignoreCase = true) -> true
            else -> false
        }
    }

    override fun getPolicy(): FileEditorPolicy = myPolicy

	override fun createEditor(
		project: com.intellij.openapi.project.Project,
		virtualFile: com.intellij.openapi.vfs.VirtualFile
	): com.intellij.openapi.fileEditor.FileEditor {
        val fileToEdit = KorgeFileToEdit(virtualFile, project)
		return KorgeBaseKorgeFileEditor(project, fileToEdit, createModule(fileToEdit), "Preview")
	}

    open fun createModule(fileToEdit: KorgeFileToEdit): Module {
        val file = fileToEdit.file

        val computedExtension = when {
            file.baseName.endsWith("_ske.json") -> "dbbin"
            else -> file.extensionLC
        }

        initializeIdeaComponentFactory()

        return runBlocking {
            try {
                when (computedExtension) {
                    "dbbin" -> dragonBonesEditor(file)
                    "skel" -> spineEditor(file)
                    "tmx" -> createModule(null) { tiledMapEditor(file) }
                    "svg" -> createModule(null) { sceneView += VectorImage(file.readSVG()).also {
                        //it.useNativeRendering = false
                    } }
                    "pex" -> particleEmiterEditor(file)
                    "ktree" -> ktreeEditor(fileToEdit)
                    "wav", "mp3", "ogg", "lipsync" -> createModule(null) { audioFileEditor(file) }
                    "swf", "ani" -> swfAnimationEditor(file)
                    else -> createModule(null) { }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                createModule(null) {
                    sceneView.text("Error: ${e.message}").centerOnStage()
                }
            }
        }
    }

	override fun getEditorTypeId(): String = this::class.java.name
}

data class BlockToExecute(val block: suspend EditorScene.() -> Unit)
