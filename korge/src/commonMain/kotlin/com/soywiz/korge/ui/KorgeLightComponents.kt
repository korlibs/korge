package com.soywiz.korge.ui

import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korio.lang.*
import com.soywiz.korui.event.*
import com.soywiz.korui.input.*
import com.soywiz.korui.light.*
import kotlin.reflect.*

//class KorgeLightComponentsFactory : LightComponentsFactory() {
//	//override fun create(): LightComponents = KorgeLightComponents()
//	override fun create(): LightComponents = TODO()
//}

class KorgeLightComponents(val uiFactory: UIFactory) : LightComponents() {
	val views = uiFactory.views

	override fun create(type: LightType): LightComponentInfo {
		val handle = when (type) {
			LightType.BUTTON -> uiFactory.button()
			LightType.CONTAINER -> FixedSizeContainer()
			LightType.FRAME -> FixedSizeContainer()
			LightType.LABEL -> uiFactory.label("")
			else -> FixedSizeContainer()
		}
		return LightComponentInfo(handle)
	}

	override fun setBounds(c: Any, x: Int, y: Int, width: Int, height: Int) {
		val view = c as View
		view.x = x.toDouble()
		view.y = y.toDouble()
		view.width = width.toDouble()
		view.height = height.toDouble()
	}

	override fun <T> setProperty(c: Any, key: LightProperty<T>, value: T) {
		val view = c as View

		when (key) {
			LightProperty.TEXT -> {
				(view as? IText)?.text = value as String
			}
		}
	}

	override fun <T : Event> registerEventKind(c: Any, clazz: KClass<T>, ed: EventDispatcher): Closeable {
		val view = c as View
		val mouseEvent = MouseEvent()
		when (clazz) {
			MouseEvent::class -> {
				return listOf(
					view.mouse.onClick {
						ed.dispatch(mouseEvent.apply {
							this.type = MouseEvent.Type.CLICK
							this.button = MouseButton.LEFT
							this.x = 0
							this.y = 0
						})
					}
				).closeable()
			}
		}
		return super.registerEventKind(c, clazz, ed)
	}

	override fun openURL(url: String) {
		//browser.browse(URL(url))
	}

	override fun setParent(c: Any, parent: Any?) {
		val view = c as View
		val parentView = parent as? Container?
		parentView?.addChild(view)
	}

	override fun repaint(c: Any) {
	}
}
