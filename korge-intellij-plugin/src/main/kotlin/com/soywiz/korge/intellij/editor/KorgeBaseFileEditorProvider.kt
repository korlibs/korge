package com.soywiz.korge.intellij.editor

import com.soywiz.korge.input.*
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
		return KorgeBaseKorgeFileEditor(project, virtualFile, EditorModule, "Preview")
	}

	override fun getEditorTypeId(): String = this::class.java.name

	object EditorModule : Module() {
		override val mainScene: KClass<out Scene> = EditorScene::class

		override suspend fun AsyncInjector.configure() {
			get<ResourcesRoot>().mount("/", pluginResurcesVfs.root)
		}
	}

    @Prototype
    class EditorScene(
        val fileToEdit: KorgeFileToEdit
    ) : Scene() {

        override suspend fun Container.sceneMain() {
            views.setVirtualSize(fileToEdit.awtComponent.width, fileToEdit.awtComponent.height)

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
            val file = fileToEdit.file

            val computedExtension = when {
                file.baseName.endsWith("_ske.json") -> "dbbin"
                else -> file.extensionLC
            }

            try {
                when (computedExtension) {
                    "dbbin" -> dragonBonesEditor(file)
                    "skel" -> spineEditor(file)
                    "tmx" -> tiledMapEditor(file)
                    "svg" -> run { sceneView += Image(file.readBitmapSlice()) }
                    //"scml" -> {
                    //	val spriterLibrary = file.readSpriterLibrary(views)
                    //	val spriterView = spriterLibrary.create(spriterLibrary.entityNames.first()).apply {
                    //		x = views.virtualWidth * 0.5
                    //		y = views.virtualHeight * 0.5
                    //	}
                    //	sceneView += spriterView
                    //}
                    "pex" -> particleEmiterEditor(file)
                    "wav", "mp3", "ogg", "lipsync" -> audioFileEditor(file)
                    "swf", "ani" -> swfAnimationEditor(file)
                }
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
                        println("OPEN: ${file.absolutePath}")
                    }
                }
                Unit
            }

            Unit
        }



    }
}
