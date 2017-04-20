package com.soywiz.korge

import com.soywiz.korag.AG
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
import com.soywiz.korio.util.TimeProvider
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korui.CanvasApplication

object Korge {
	val VERSION = "0.8.3"

	suspend fun setupCanvas(
		container: AGContainer,
		module: Module,
		args: Array<String> = arrayOf(),
		sceneClass: Class<out Scene> = module.mainScene,
		timeProvider: TimeProvider = TimeProvider(),
		injector: AsyncInjector = AsyncInjector()
	): SceneContainer {
		val ag = container.ag
		injector.mapTyped<AG>(ag)
		val views = injector.get<Views>()
		val moduleArgs = ModuleArgs(args)

		views.virtualWidth = module.width
		views.virtualHeight = module.height

		ag.onReady.await()
		injector.mapTyped(moduleArgs)
		injector.mapTyped(timeProvider)
		injector.mapTyped<Module>(module)
		module.init(injector)

		fun updateMousePos() {
			val mouseX = container.mouseX.toDouble()
			val mouseY = container.mouseY.toDouble()
			//println("updateMousePos: $mouseX, $mouseY")
			views.input.mouse.setTo(mouseX, mouseY)
			views.mouseUpdated()
		}

		container.onMouseOver {
			updateMousePos()
		}
		container.onMouseUp {
			views.input.mouseButtons = 0
			updateMousePos()
		}
		container.onMouseDown {
			views.input.mouseButtons = 1
			updateMousePos()
		}
		ag.onResized {
			views.resized(ag.backWidth, ag.backHeight)
		}
		ag.resized()

		var lastTime = timeProvider.currentTimeMillis()
		//println("lastTime: $lastTime")
		ag.onRender {
			//println("Render")
			ag.clear(module.bgcolor)
			val currentTime = timeProvider.currentTimeMillis()
			//println("currentTime: $currentTime")
			val delta = (currentTime - lastTime).toInt()
			val adelta = Math.min(delta, views.clampElapsedTimeTo)
			//println("delta: $delta")
			//println("Render($lastTime -> $currentTime): $delta")
			lastTime = currentTime
			views.update(adelta)
			views.render()
			//println("render:$delta,$adelta")
		}

		animationFrameLoop {
			//ag.resized()
			container.repaint()
		}

		val sc = views.sceneContainer()
		views.stage += sc
		sc.changeTo(sceneClass, time = 0)

		return sc
	}

	operator fun invoke(
		module: Module,
		args: Array<String> = arrayOf(),
		canvas: AGContainer? = null,
		sceneClass: Class<out Scene> = module.mainScene,
		timeProvider: TimeProvider = TimeProvider(),
		injector: AsyncInjector = AsyncInjector(),
		debug: Boolean = false
	) = EventLoop {
		test(module = module, args = args, canvas = canvas, sceneClass = sceneClass, injector = injector, timeProvider = timeProvider, debug = debug)
	}

	suspend fun test(
		module: Module,
		args: Array<String> = arrayOf(),
		canvas: AGContainer? = null,
		sceneClass: Class<out Scene> = module.mainScene,
		injector: AsyncInjector = AsyncInjector(),
		timeProvider: TimeProvider = TimeProvider(),
		debug: Boolean = false
	): SceneContainer {
		val done = Promise.Deferred<SceneContainer>()
		if (canvas != null) {
			done.resolve(setupCanvas(container = canvas, module = module, args = args, sceneClass = sceneClass, timeProvider = timeProvider, injector = injector))
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
					done.resolve(setupCanvas(container = it, module = module, args = args, sceneClass = sceneClass, timeProvider = timeProvider, injector = injector))
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
