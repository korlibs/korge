package com.soywiz.korge.intellij.editor.tiled.dialog

import com.intellij.ui.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.intellij.ui.*
import com.soywiz.korge.intellij.util.ObservableProperty
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import java.awt.event.*
import javax.swing.event.*

fun showResizeMapDialog(initialWidth: Int, initialHeight: Int): SizeInt? {
	val mapWidth = ObservableProperty("$initialWidth")
	val mapHeight = ObservableProperty("$initialHeight")
	val initializedSignal = Signal<Unit>()

	val result = showDialog("Resize map") {
		verticalStack {
			horizontalStack {
				height = 32.pt
				label("Width:") {
					width = 50.percentage
					height = 32.pt
				}
				textField("$initialWidth") {
					width = 50.percentage
					height = 32.pt
					component.addFocusListener(object : FocusAdapter() {
						override fun focusGained(e: FocusEvent?) {
							component.selectAll()
						}
					})
					component.document.addDocumentListener(object : DocumentAdapter() {
						override fun textChanged(e: DocumentEvent) {
							mapWidth.value = component.text
						}
					})
					initializedSignal {
						component.grabFocus()
					}
				}
			}
			horizontalStack {
				height = 32.pt
				label("Height:") {
					width = 50.percentage
					height = 32.pt
				}
				textField("$initialHeight") {
					width = 50.percentage
					height = 32.pt
					component.document.addDocumentListener(object : DocumentAdapter() {
						override fun textChanged(e: DocumentEvent) {
							mapHeight.value = component.text
						}
					})
					component.addFocusListener(object : FocusAdapter() {
						override fun focusGained(e: FocusEvent?) {
							component.selectAll()
						}
					})
				}
			}
		}
		initializedSignal(Unit)
	}

	return when {
		result -> SizeInt(mapWidth.value.trim().toInt(), mapHeight.value.trim().toInt())
		else -> null
	}
}
