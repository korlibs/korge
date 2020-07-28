package com.soywiz.korge.intellij.editor

import com.soywiz.korge.animate.*
import com.soywiz.korge.animate.serialization.*
import com.soywiz.korge.ext.swf.*
import com.soywiz.korge.input.*
import com.soywiz.korge.lipsync.*
import com.soywiz.korge.particle.*
import com.soywiz.korge.resources.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.tiled.*
import com.soywiz.korge.time.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.vector.*
import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import kotlin.reflect.*

abstract class KorgeBaseFileEditorProvider : com.intellij.openapi.fileEditor.FileEditorProvider {
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

		class EditorScene(
			val fileToEdit: KorgeFileToEdit
		) : Scene() {
			private suspend fun getLipTexture(char: Char) =
				runCatching { pluginResurcesVfs["/com/soywiz/korge/intellij/lips/lisa-$char.png"].readBitmapSlice() }.getOrNull()
					?: Bitmaps.transparent

			override suspend fun Container.sceneInit() {
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

				views.launchImmediately {
					delayFrame()
					val file = fileToEdit.file

					when (file.extensionLC) {
						"tmx" -> {
							val tiled = file.readTiledMap()
							sceneView += tiled.createView()
							views.setVirtualSize(tiled.pixelWidth, tiled.pixelHeight)
						}
						"svg" -> {
							sceneView += Image(file.readBitmapSlice())
						}
						//"scml" -> {
						//	val spriterLibrary = file.readSpriterLibrary(views)
						//	val spriterView = spriterLibrary.create(spriterLibrary.entityNames.first()).apply {
						//		x = views.virtualWidth * 0.5
						//		y = views.virtualHeight * 0.5
						//	}
						//	sceneView += spriterView
						//}
						"pex" -> {
							sceneView += fileToEdit.file.readParticle()
								.create(views.virtualWidth / 2.0, views.virtualHeight / 2.0)
						}
						"wav", "mp3", "ogg", "lipsync" -> {
							var voice: Voice? = null
							val voiceName = "voice"

							if (file.baseName.contains("voice") || file.baseName.contains("lipsync")) {
								val wav = file.withExtension("wav")
								val mp3 = file.withExtension("mp3")
								val ogg = file.withExtension("ogg")
								val audios = listOf(wav, mp3, ogg)
								val audio = audios.firstOrNull { it.exists() }
								voice = audio?.readVoice()
								//audio?.readAudioData()?.play()

								//val classLoader = pluginClassLoader

								views.setVirtualSize(408 * 2, 334 * 2)

								val mouth = AnSimpleAnimation(
									10, mapOf(
										"A" to listOf(getLipTexture('A')),
										"B" to listOf(getLipTexture('B')),
										"C" to listOf(getLipTexture('C')),
										"D" to listOf(getLipTexture('D')),
										"E" to listOf(getLipTexture('E')),
										"F" to listOf(getLipTexture('F')),
										"G" to listOf(getLipTexture('G')),
										"H" to listOf(getLipTexture('H')),
										"X" to listOf(getLipTexture('X'))
									), Anchor.MIDDLE_CENTER
								).apply {
									x = views.virtualWidth * 0.5
									y = views.virtualHeight * 0.5
									addProp("lipsync", voiceName)
								}

								sceneView += mouth
							}

							fun stopSound() {
								//promise?.cancel()
								println("stopSound")
							}

							fun playSound() {
								println("playSound")
								/*
								promise?.cancel()
								promise = go(views.coroutineContext) {
									if (voice != null) {
										voice?.play(voiceName)
									} else {
										views.soundSystem.play(file.readNativeSoundOptimized())
									}
									Unit
								}
								 */
							}

							playSound()

							//sceneView.addEventListener<LipSyncEvent> { e ->
							//	mouth.tex = lips[e.lip] ?: views.transparentTexture
							//}

							sceneView.textButton(text = "Replay").apply {
								width = 80.0
								height = 24.0
								x = 0.0
								y = 0.0
								onClick {
									playSound()
								}
							}

							sceneView.textButton(text = "Stop").apply {
								width = 80.0
								height = 24.0
								x = 80.0
								y = 0.0
								onClick {
									stopSound()
								}
							}

							Unit
						}
						"swf", "ani" -> {
							val animationLibrary = when (file.extensionLC) {
								"swf" -> file.readSWF(
									views, defaultConfig = SWFExportConfig(
										mipmaps = false,
										antialiasing = true,
										rasterizerMethod = ShapeRasterizerMethod.X4,
										exportScale = 1.0,
										exportPaths = false
									)
								)
								"ani" -> file.readAni(views)
								else -> null
							}

							if (animationLibrary != null) {
								views.setVirtualSize(animationLibrary.width, animationLibrary.height)
								sceneView += animationLibrary.createMainTimeLine()
							}

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
						}
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
	}
}
