package com.soywiz.korge.newui

import com.soywiz.korge.html.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.properties.*

inline fun Container.uiCheckBox(
	checked: Boolean? = false,
	width: Number = 96.0,
	height: Number = 32.0,
	label: String = "CheckBox",
	skin: UISkin = defaultUISkin,
	block: UICheckBox.() -> Unit = {}
): UICheckBox = UICheckBox(checked, width.toDouble(), height.toDouble(), label, skin).also { addChild(it) }.apply(block)

open class UICheckBox(
	checked: Boolean? = false,
	width: Double = 96.0,
	height: Double = 32.0,
	label: String = "CheckBox",
	private val skin: UISkin = DefaultUISkin
) : UIView(width, height) {
	var checked by uiObservable(checked) { onPropsUpdate() }
	var label by uiObservable(label) { onPropsUpdate() }

	private val area = solidRect(16, 16, Colors.TRANSPARENT_BLACK)
	//private val box = solidRect(16, 16, Colors.DARKGREY)
	private val box = uiButton(16, 16, skin = skin).also { it.mouseEnabled = false }
	private val text = text(label)

	init {
		onClick {
			this@UICheckBox.checked = this@UICheckBox.checked != true
			onPropsUpdate()
		}
		onPropsUpdate()
	}

	protected fun onPropsUpdate() {
		println("checked: $checked")
		area.position(0, 0).size(width, height)
		box.position(0, 0).size(height, height)
			.also {
				it.forcePressed = true
				if (checked == true) {
					it.label = "X"
				} else {
					it.label = ""
				}
			}
		text.position(height + 8.0, 0)
			.also { it.format = Html.Format(face = skin.font, align = Html.Alignment.MIDDLE_LEFT) }
			.also { it.setTextBounds(Rectangle(0, 0, width - height, height)) }
			.setText(label)
	}
}
