package com.soywiz.korge.intellij.editor

import com.intellij.execution.console.*
import com.soywiz.korge.input.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.components.*
import com.soywiz.korge.intellij.editor.formats.*
import com.soywiz.korge.resources.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.time.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import kotlin.reflect.*

abstract class KorgeBaseFileEditorProvider : com.intellij.openapi.fileEditor.FileEditorProvider, com.intellij.openapi.project.DumbAware {
	companion object {
		val pluginClassLoader by lazy { KorgeBaseFileEditorProvider::class.java.classLoader }
		//val pluginResurcesVfs by lazy { resourcesVfs(pluginClassLoader) }
		val pluginResurcesVfs by lazy { resourcesVfs }
	}

	override fun createEditor(
		project: com.intellij.openapi.project.Project,
		virtualFile: com.intellij.openapi.vfs.VirtualFile
	): com.intellij.openapi.fileEditor.FileEditor {
        val fileToEdit = KorgeFileToEdit(virtualFile)
		return KorgeBaseKorgeFileEditor(project, virtualFile, createModule(fileToEdit), "Preview")
	}

    open fun createModule(fileToEdit: KorgeFileToEdit): KorgeBaseKorgeFileEditor.EditorModule {
        val file = fileToEdit.file

        val computedExtension = when {
            file.baseName.endsWith("_ske.json") -> "dbbin"
            else -> file.extensionLC
        }

        return when (computedExtension) {
            "dbbin" -> createModule(null) { dragonBonesEditor(file) }
            "skel" -> createModule(null) { spineEditor(file) }
            "tmx" -> createModule(null) { tiledMapEditor(file) }
            "svg" -> createModule(null) {  sceneView += Image(file.readBitmapSlice()) }
            "pex" -> particleEmiterEditor(file)
            "wav", "mp3", "ogg", "lipsync" -> createModule(null) { audioFileEditor(file) }
            "swf", "ani" -> createModule(null) { swfAnimationEditor(file) }
            else -> createModule(null) { }
        }
    }

	override fun getEditorTypeId(): String = this::class.java.name

    data class BlockToExecute(val block: suspend Scene.() -> Unit)

    @Prototype
    class EditorScene(
        val fileToEdit: KorgeFileToEdit,
        val blockToExecute: BlockToExecute
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

            sceneView.textButton(text = "Open").apply {
                width = 80.0
                height = 24.0
                x = views.virtualWidth - width
                y = 0.0
                onClick {
                    views.launchImmediately {
                        //views.gameWindow.openFileDialog(LocalVfs(file.absolutePath))
                        println("OPEN: ${fileToEdit.file.absolutePath}")
                    }
                }
                Unit
            }

            Unit
        }
    }
}

fun createModule(editableNode: EditableNode?, block: suspend Scene.() -> Unit): KorgeBaseKorgeFileEditor.EditorModule {
    return object : KorgeBaseKorgeFileEditor.EditorModule() {
        override val editableNode: EditableNode? get() = editableNode
        override val mainScene: KClass<out Scene> = KorgeBaseFileEditorProvider.EditorScene::class
        override suspend fun AsyncInjector.configure() {
            get<ResourcesRoot>().mount("/", KorgeBaseFileEditorProvider.pluginResurcesVfs.root)
            mapInstance(KorgeBaseFileEditorProvider.BlockToExecute(block))
        }
    }
}
