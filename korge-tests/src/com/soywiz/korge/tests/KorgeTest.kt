package com.soywiz.korge.tests

import com.soywiz.korag.AG
import com.soywiz.korag.AGContainer
import com.soywiz.korge.Korge
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.ViewsLog
import com.soywiz.korio.async.Signal
import com.soywiz.korio.async.syncTest

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

	fun testScene(module: Module, sceneClass: Class<out Scene>, callback: suspend () -> Unit) = syncTest {
		val korge = Korge(module, sceneClass = sceneClass, canvas = canvas)
		callback()
	}
}
