package com.soywiz.korge.intellij.editor

import com.intellij.openapi.fileEditor.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.intellij.components.*
import com.soywiz.korge.intellij.editor.formats.*
import com.soywiz.korge.resources.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korinject.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.*
import kotlin.reflect.*

open class KorgeFileEditorProvider : com.intellij.openapi.fileEditor.FileEditorProvider, com.intellij.openapi.project.DumbAware {
	companion object {
		val pluginClassLoader by lazy { KorgeFileEditorProvider::class.java.classLoader }
		//val pluginResurcesVfs by lazy { resourcesVfs(pluginClassLoader) }
		val pluginResurcesVfs by lazy { resourcesVfs }
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
        val fileToEdit = KorgeFileToEdit(virtualFile)
		return KorgeBaseKorgeFileEditor(project, virtualFile, createModule(fileToEdit), "Preview")
	}

    open fun createModule(fileToEdit: KorgeFileToEdit): EditorModule {
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
                    "svg" -> createModule(null) { sceneView += Image(file.readBitmapSlice()) }
                    "pex" -> particleEmiterEditor(file)
                    "ktree" -> ktreeEditor(file)
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

    data class BlockToExecute(val block: suspend KorgeFileEditorProvider.EditorScene.() -> Unit)

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
}


fun createModule(editableNode: EditableNode? = null, block: suspend KorgeFileEditorProvider.EditorScene.() -> Unit): EditorModule {
    return object : EditorModule() {
        override val editableNode: EditableNode? get() = editableNode
        override val mainScene = KorgeFileEditorProvider.EditorScene::class
        override suspend fun AsyncInjector.configure() {
            get<ResourcesRoot>().mount("/", KorgeFileEditorProvider.pluginResurcesVfs.root)
            mapInstance(KorgeFileEditorProvider.BlockToExecute(block))
        }
    }
}

