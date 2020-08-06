package com.soywiz.korui.react

import com.soywiz.kds.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korui.*
import com.soywiz.korui.light.*
import com.soywiz.korui.ui.*

object React

// @TODO: Virtual DOM and reuse components
abstract class ReactComponent<TState : Any> {
	private lateinit var virtualApp: Application
	private lateinit var virtualRoot: Container
	private lateinit var root: Container

	private lateinit var _state: TState

	var state: TState
		get() = _state
		set(value) {
			_state = value
			launchImmediately(virtualApp.coroutineContext) {
				//val diff = true
				val diff = false
				if (diff) {
					virtualRoot.removeAll()
					virtualRoot.render()
					virtualRoot.synchronizeTo(root)
				} else {
					root.removeAll()
					root.render()
				}
				root.relayout()
			}
		}

	abstract suspend fun Container.render()

	fun React.attach(root: Container, initialState: TState) {
		this@ReactComponent.virtualApp = Application(root.app.coroutineContext, DummyLightComponents)
		this@ReactComponent.virtualRoot = root.clone(virtualApp)
		this@ReactComponent.root = root
		this@ReactComponent.state = initialState
	}
}

fun <TState : Any> Container.attachReactComponent(component: ReactComponent<TState>, state: TState) {
	component.apply {
		React.attach(this@attachReactComponent, state)
	}
}

suspend fun <TState : Any> Application.reactFrame(
	component: ReactComponent<TState>,
	state: TState,
	title: String,
	width: Int = 640,
	height: Int = 480,
	icon: Bitmap? = null
): Frame {
	return frame(title, width, height, icon) {
		attachReactComponent(component, state)
	}
}

////////////////////////////////////////


class ReactUpdater(val real: Container, val virtualNext: Container) {
	private val realComponentsByKey = real.getComponentsByKey()

	init {
		//println(realComponentsByKey)
	}

	fun reactUpdate() {
		real.copyStateFrom(virtualNext)
		reactUpdate(real, virtualNext)
	}

	fun reactUpdate(real: Container, virtualNext: Container) {
		val realVirtualZip = real.children.zip(virtualNext.children)
		// For same size and same type, let's reuse all the children
		if (real.children.size == virtualNext.children.size && realVirtualZip.all { it.first.toString() == it.second.toString() }) {
			for ((realChild, virtualChild) in realVirtualZip) {
				realChild.copyStateFrom(virtualChild)
				if (realChild is Container) {
					reactUpdate(realChild, virtualChild as Container)
				}
			}
		}
		// For different size, let's reuse only keys
		else {
			real.removeAll()
			for (virtualChild in virtualNext.children) {
				val realChild = if (virtualChild.key in realComponentsByKey) {
					realComponentsByKey[virtualChild.key]!!.apply { copyStateFrom(virtualChild) }
				} else {
					virtualChild.clone(real.app)
				}

				if (realChild is Container) {
					reactUpdate(realChild, virtualChild as Container)
				}
				real.add(realChild)
			}
		}
	}
}

fun Container.synchronizeTo(real: Container) {
	ReactUpdater(real, this).reactUpdate()
}

fun Component.getComponentsByKey(out: LinkedHashMap<String, Component> = LinkedHashMap()): Map<String, Component> {
	if (this.key != null) out[this.key!!] = this
	if (this is Container) for (child in this.children) child.getComponentsByKey(out)
	return out
}

object DummyLightComponents : LightComponents()

// React usage for diffing
var Component.key: String? by extraProperty("react.key") { null }
