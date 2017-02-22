@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korge

import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.sceneContainer
import com.soywiz.korge.view.Views
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.go
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.inject.Singleton
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korui.Application
import com.soywiz.korui.frame
import com.soywiz.korui.ui.agCanvas

object Korge {
    val VERSION = "0.6.0"

    operator fun invoke(module: Module, args: Array<String> = arrayOf(), injector: AsyncInjector = AsyncInjector()) = EventLoop {
        Application().frame(module.title) {
            if (module.icon != null) {
                try {
                    icon = ResourcesVfs[module.icon!!].readBitmap()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }

            val canvas = agCanvas { }
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

            var lastTime = System.currentTimeMillis()
            ag.onRender {
                ag.clear(module.bgcolor)
                val currentTime = System.currentTimeMillis()
                val delta = (currentTime - lastTime).toInt()
                lastTime = currentTime
                views.update(delta)
                views.render()
            }

            canvas.onClick {
                views.input.mouse.setTo(canvas.mouseX.toDouble(), canvas.mouseY.toDouble())
            }
            canvas.onOver {
                views.input.mouse.setTo(canvas.mouseX.toDouble(), canvas.mouseY.toDouble())
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
