package com.soywiz.korge.ui.korui

import com.soywiz.korge.input.mouse
import com.soywiz.korge.ui.UIFactory
import com.soywiz.korge.ui.button
import com.soywiz.korge.ui.label
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.IText
import com.soywiz.korge.view.View
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.util.closeable
import com.soywiz.korui.light.LightComponents
import com.soywiz.korui.light.LightMouseHandler
import com.soywiz.korui.light.LightProperty
import com.soywiz.korui.light.LightType

//class KorgeLightComponentsFactory : LightComponentsFactory() {
//	//override fun create(): LightComponents = KorgeLightComponents()
//	override fun create(): LightComponents = TODO()
//}

class KorgeLightComponents(val uiFactory: UIFactory) : LightComponents() {
	val views = uiFactory.views

	override fun create(type: LightType): LightComponentInfo {
		val handle = when (type) {
			LightType.BUTTON -> uiFactory.button()
			LightType.CONTAINER -> views.fixedSizeContainer()
			LightType.FRAME -> views.fixedSizeContainer()
			LightType.LABEL -> uiFactory.label("")
			else -> views.fixedSizeContainer()
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

	override fun addHandler(c: Any, listener: LightMouseHandler): Closeable {
		val view = c as View
		val info = LightMouseHandler.Info()

		return listOf(
			view.mouse.onClick { listener.click(info) }
		).closeable()
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
