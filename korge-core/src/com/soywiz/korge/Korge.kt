@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korge

import com.soywiz.korag.AGContainer
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.sceneContainer
import com.soywiz.korge.view.Views
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.go
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.inject.Singleton
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korui.CanvasApplication
import com.soywiz.korui.ui.AgCanvas

object Korge {
    val VERSION = "0.8.0"

    suspend fun setupCanvas(canvas: AGContainer, module: Module, args: Array<String> = arrayOf(), injector: AsyncInjector = AsyncInjector()) {
        val ag = canvas.ag
        injector.map(ag)
        val views = injector.get<Views>()

        ag.onReady {
            go {
                val sc = views.sceneContainer()
                views.root += sc
                sc.changeTo(module.mainScene)

                animationFrameLoop {
                    canvas.repaint()
                }
            }
        }

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
    }

    operator fun invoke(
		module: Module,
		args: Array<String> = arrayOf(),
		canvas: AgCanvas? = null,
		injector: AsyncInjector = AsyncInjector(),
		debug: Boolean = false
	) = EventLoop {
        if (canvas != null) {
            setupCanvas(canvas, module, args, injector)
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
                setupCanvas(it, module, args, injector)
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
