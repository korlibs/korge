@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korge

import com.soywiz.korag.AGContainer
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.scene.SceneContainer
import com.soywiz.korge.scene.sceneContainer
import com.soywiz.korge.view.Views
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.Promise
import com.soywiz.korio.async.go
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korui.CanvasApplication

object Korge {
	val VERSION = "0.8.0"

	suspend fun setupCanvas(canvas: AGContainer, module: Module, args: Array<String> = arrayOf(), sceneClass: Class<out Scene> = module.mainScene, injector: AsyncInjector = AsyncInjector()): SceneContainer {
		val ag = canvas.ag
		injector.map(ag)
		val views = injector.get<Views>()
		val moduleArgs = ModuleArgs(args)

		ag.onReady.await()
		injector.map(moduleArgs)
		module.init(injector)

		val sc = views.sceneContainer()
		views.root += sc
		sc.changeTo(sceneClass)

		var lastTime = System.currentTimeMillis()
		ag.onRender {
			ag.clear(module.bgcolor)
			val currentTime = System.currentTimeMillis()
			val delta = (currentTime - lastTime).toInt()
			lastTime = currentTime
			views.update(delta)
			views.render()
		}

		fun updateMousePos() {
			views.input.mouse.setTo(canvas.mouseX.toDouble(), canvas.mouseY.toDouble())
		}

		canvas.onMouseOver {
			updateMousePos()
		}
		canvas.onMouseUp {
			views.input.mouseButtons = 0
			updateMousePos()
		}
		canvas.onMouseDown {
			views.input.mouseButtons = 1
			updateMousePos()
		}

		animationFrameLoop {
			canvas.repaint()
		}

		return sc
	}

	operator fun invoke(
		module: Module,
		args: Array<String> = arrayOf(),
		canvas: AGContainer? = null,
		sceneClass: Class<out Scene> = module.mainScene,
		injector: AsyncInjector = AsyncInjector(),
		debug: Boolean = false
	) = EventLoop {
		test(module, args, canvas, sceneClass, injector, debug)
	}

	suspend fun test(
		module: Module,
		args: Array<String> = arrayOf(),
		canvas: AGContainer? = null,
		sceneClass: Class<out Scene> = module.mainScene,
		injector: AsyncInjector = AsyncInjector(),
		debug: Boolean = false
	): SceneContainer {
		val done = Promise.Deferred<SceneContainer>()
		if (canvas != null) {
			done.resolve(setupCanvas(canvas, module, args, sceneClass, injector))
		} else {
			val icon = if (module.icon != null) {
				try {
					ResourcesVfs[module.icon!!].readBitmap()
				} catch (e: Throwable) {
					e.printStackTrace()
					null
				}
			} else {
				null
			}

			CanvasApplication(module.title, module.width, module.height, icon) {
				go {
					done.resolve(setupCanvas(it, module, args, sceneClass, injector))
				}
			}
		}
		return done.promise.await()
	}

	fun animationFrameLoop(callback: () -> Unit) {
		var step: (() -> Unit)? = null
		step = {
			callback()
			EventLoop.requestAnimationFrame(step!!)
		}
		step()
	}

	data class ModuleArgs(val args: Array<String>)
}
