package com.soywiz.korge

import com.soywiz.korag.AG
import com.soywiz.korge.render.Texture
import com.soywiz.korge.render.readTexture
import com.soywiz.korge.resources.Path
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.sceneContainer
import com.soywiz.korge.view.Views
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.go
import com.soywiz.korio.inject.AsyncDependency
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.inject.Prototype
import com.soywiz.korio.inject.Singleton
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korui.Application
import com.soywiz.korui.frame
import com.soywiz.korui.ui.agCanvas

object Korge {
	val VERSION = "0.5.3"

	operator fun invoke(module: Module, args: Array<String> = arrayOf()) = EventLoop.main {
		val injector = AsyncInjector()

		Application().frame(module.title) {
			if (module.icon != null) {
				icon = ResourcesVfs[module.icon!!].readBitmap()
			}

			val canvas = agCanvas {
			}

			val ag = canvas.ag
			injector.map(ag)
			val views = injector.get<Views>()

			ag.onReady {
				go {
					val sc = views.sceneContainer()
					views.root += sc
					sc.changeToScene(module.mainScene)

					animationFrameLoop {
						canvas.repaint()
					}
				}
			}

			ag.onRender {
				ag.clear(module.bgcolor)
				views.render()
			}

			canvas.onClick {
				views.mouse.setTo(canvas.mouseX.toDouble(), canvas.mouseY.toDouble())
			}
			canvas.onOver {
				views.mouse.setTo(canvas.mouseX.toDouble(), canvas.mouseY.toDouble())
			}
		}
	}

	fun animationFrameLoop(callback: () -> Unit) {
		var step: (() -> Unit)? = null
		step = {
			callback()
			EventLoop.requestAnimationFrame(step!!)
		}
		step()
	}
}

@Singleton
class ResourcesRoot {
}
