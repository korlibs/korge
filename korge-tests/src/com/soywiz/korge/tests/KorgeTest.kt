package com.soywiz.korge.tests

import com.soywiz.korag.AG
import com.soywiz.korag.AGContainer
import com.soywiz.korge.Korge
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.ViewsLog
import com.soywiz.korim.format.disableNativeImageLoading
import com.soywiz.korio.async.Signal
import com.soywiz.korio.async.syncTest

@Suppress("unused")
open class KorgeTest {
	val viewsLogs = ViewsLog()
	val ag = viewsLogs.ag
	val canvas = object : AGContainer {
		override val ag: AG = viewsLogs.ag

		override val mouseX: Int = 0
		override val mouseY: Int = 0
		override val onMouseOver: Signal<Unit> = Signal()
		override val onMouseUp: Signal<Unit> = Signal()
		override val onMouseDown: Signal<Unit> = Signal()

		override fun repaint(): Unit {
		}
	}
	val views = viewsLogs.views

	@Suppress("UNCHECKED_CAST")
	fun <T : Scene> testScene(module: Module, sceneClass: Class<T>, callback: suspend T.() -> Unit) = syncTest {
		disableNativeImageLoading {
			val sc = Korge.test(module, sceneClass = sceneClass, canvas = canvas)
			callback(sc.currentScene as T)
		}
	}

	fun Scene.updateTime(dtMs: Int = 20) = views.update(dtMs)
}
