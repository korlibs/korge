package com.soywiz.korge.intellij.util

import java.awt.*

inline fun Graphics2D.preserveStroke(block: () -> Unit) {
	val old = stroke
	try {
		block()
	} finally {
		stroke = old
	}
}

inline fun Graphics2D.preserveTransform(block: () -> Unit) {
	val old = transform
	try {
		block()
	} finally {
		transform = old
	}
}
